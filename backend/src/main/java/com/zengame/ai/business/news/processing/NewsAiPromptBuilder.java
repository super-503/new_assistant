package com.zengame.ai.business.news.processing;

import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldSource;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据加工的提示词组装：决定一条资讯有哪些字段参与生成，并把这些字段组装成一次模型调用的提示词。
 */
@Component
public class NewsAiPromptBuilder {

    /**
     * 交给模型的正文纯文本上限（字符）
     */
    public static final int BODY_MAX_CHARS = 8000;

    private static final String NO_TITLE = "（无标题）";

    private static final String NO_BODY = "（无正文）";

    /**
     * 筛出一条资讯参与生成的字段，规则见 {@link #isApplicable}。
     *
     * @param fields      候选字段，按展示顺序
     * @param contentType 资讯类型
     * @return 参与生成的字段，保持原顺序
     */
    public List<NewsAiFieldPO> selectApplicable(List<NewsAiFieldPO> fields, NewsContentType contentType) {
        return fields.stream()
                .filter(field -> isApplicable(field, contentType))
                .collect(Collectors.toList());
    }

    /**
     * 字段是否参与这类资讯的生成：已启用；不是选项为空的单选 / 多选；视频不生成只对文章生成的字段。
     *
     * @param field       AI 字段
     * @param contentType 资讯类型
     * @return 参与生成返回 true
     */
    public boolean isApplicable(NewsAiFieldPO field, NewsContentType contentType) {
        if (BooleanUtils.isNotTrue(field.getEnabled())) {
            return false;
        }
        if (contentType == NewsContentType.VIDEO && BooleanUtils.isTrue(field.getArticleOnly())) {
            return false;
        }
        return !isChoice(field.getOutputType()) || !StringListJson.read(field.getOptionsJson()).isEmpty();
    }

    /**
     * 组装一条资讯的加工提示词：资讯内容只出现一次，逐个字段写明依据、输出类型、可选项和字段提示词，要求输出以字段标识为键的 JSON 对象。
     *
     * @param item   资讯
     * @param fields 参与生成的字段，不能为空
     * @return 提示词
     */
    public String build(NewsItemPO item, List<NewsAiFieldPO> fields) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是资讯结构化助手。阅读下面的资讯，按「字段要求」逐个生成字段，结果用于资讯审核和资讯库。\n\n");
        if (fields.stream().anyMatch(field -> field.getSource() != NewsAiFieldSource.BODY)) {
            prompt.append("## 资讯标题\n").append(titleText(item)).append("\n\n");
        }
        if (fields.stream().anyMatch(field -> field.getSource() != NewsAiFieldSource.TITLE)) {
            prompt.append("## 资讯正文\n").append(bodyText(item)).append("\n\n");
        }
        prompt.append("## 字段要求\n");
        for (int i = 0; i < fields.size(); i++) {
            appendField(prompt, i + 1, fields.get(i));
        }
        String keys = fields.stream().map(NewsAiFieldPO::getFieldKey).collect(Collectors.joining("、"));
        prompt.append("\n## 输出格式\n")
                .append("只输出一个 JSON 对象，不要输出代码块标记、解释或其他内容。\n")
                .append("JSON 对象的键是字段标识，必须包含且只包含：").append(keys).append("。\n")
                .append("值按字段的输出类型填写：文本为字符串；数字为 JSON 数字，不加引号；")
                .append("单选为一个字符串，必须与可选项之一完全一致；多选为字符串数组，元素必须与可选项完全一致，没有合适的选项时为空数组。\n");
        return prompt.toString();
    }

    /**
     * 写入一个字段的要求。
     */
    private void appendField(StringBuilder prompt, int index, NewsAiFieldPO field) {
        prompt.append(index).append(". ").append(field.getFieldKey()).append("（").append(field.getName()).append("）\n")
                .append("   - 依据：").append(sourceText(field.getSource())).append("\n")
                .append("   - 输出类型：").append(outputTypeText(field)).append("\n");
        if (isChoice(field.getOutputType())) {
            prompt.append("   - 可选项：").append(JacksonUtil.writeValueAsString(StringListJson.read(field.getOptionsJson())))
                    .append("\n");
        }
        prompt.append("   - 要求：").append(field.getPrompt()).append("\n");
    }

    private String sourceText(NewsAiFieldSource source) {
        switch (source) {
            case BODY:
                return "资讯正文";
            case TITLE:
                return "资讯标题";
            case TITLE_BODY:
                return "资讯标题和正文";
            default:
                throw new IllegalStateException("未知的来源内容: " + source);
        }
    }

    private String outputTypeText(NewsAiFieldPO field) {
        switch (field.getOutputType()) {
            case TEXT:
                return BooleanUtils.isTrue(field.getMultiline())
                        ? "多行文本，输出一个字符串，可以用换行分段"
                        : "单行文本，输出一个字符串，不换行";
            case NUMBER:
                return "数字，输出一个 JSON 数字";
            case SINGLE:
                return "单选，输出一个字符串，只能取可选项之一";
            case MULTI:
                return "多选，输出字符串数组，元素只能取自可选项，可以为空数组";
            default:
                throw new IllegalStateException("未知的输出类型: " + field.getOutputType());
        }
    }

    private String titleText(NewsItemPO item) {
        return StringUtils.defaultIfBlank(StringUtils.trim(item.getOriginTitle()), NO_TITLE);
    }

    /**
     * 正文去掉标签后的纯文本，截断到 {@link #BODY_MAX_CHARS}。
     */
    private String bodyText(NewsItemPO item) {
        String text = StringUtils.left(Jsoup.parse(StringUtils.defaultString(item.getBodyHtml())).text(), BODY_MAX_CHARS);
        return StringUtils.defaultIfBlank(text, NO_BODY);
    }

    /**
     * @param outputType 输出类型
     * @return 单选或多选返回 true
     */
    static boolean isChoice(NewsAiFieldOutputType outputType) {
        return outputType == NewsAiFieldOutputType.SINGLE || outputType == NewsAiFieldOutputType.MULTI;
    }
}
