package com.zengame.ai.business.news.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldKind;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.processing.StringListJson;
import com.zengame.ai.business.news.review.domain.bo.NewsItemBO;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * AI 字段在审核和资讯库里的展示与校验规则。
 */
public final class NewsAiFieldRules {

    private NewsAiFieldRules() {
    }

    /**
     * @param field AI 字段
     * @return 单选、多选的选项，按配置顺序
     */
    public static List<String> options(NewsAiFieldPO field) {
        return StringListJson.read(field.getOptionsJson());
    }

    /**
     * 字段对该类型的资讯是否适用：「只对文章生成」的字段不用于视频。
     *
     * @param field       AI 字段
     * @param contentType 资讯类型
     * @return 适用返回 true
     */
    public static boolean isApplicable(NewsAiFieldPO field, NewsContentType contentType) {
        return !(field.getArticleOnly() && contentType == NewsContentType.VIDEO);
    }

    /**
     * 标签字段：开启「显示为标签」的字段，以及内置的岗位分类。
     *
     * @param field AI 字段
     * @return 是标签字段返回 true
     */
    public static boolean isTagField(NewsAiFieldPO field) {
        return field.getShowAsTag() || field.getKind() == NewsAiFieldKind.BUILTIN;
    }

    /**
     * @param field AI 字段
     * @return 单选或多选返回 true
     */
    public static boolean isChoice(NewsAiFieldPO field) {
        return field.getOutputType() == NewsAiFieldOutputType.SINGLE || field.getOutputType() == NewsAiFieldOutputType.MULTI;
    }

    /**
     * 字段能否取值：选项为空的单选、多选字段不参与生成，也无从填写。
     *
     * @param field AI 字段
     * @return 能取值返回 true
     */
    public static boolean isFillable(NewsAiFieldPO field) {
        return !(isChoice(field) && options(field).isEmpty());
    }

    /**
     * 字段在这条资讯上是否未生成：能取值的字段，结果里没有或加工时记为未生成。
     *
     * @param field AI 字段
     * @param item  资讯
     * @return 未生成返回 true
     */
    public static boolean isMissing(NewsAiFieldPO field, NewsItemBO item) {
        return isFillable(field) && item.isMissing(field.getFieldKey());
    }

    /**
     * 标签配色序号：值在字段选项里的下标。岗位分类用中性样式，非标签字段不配色，都没有序号。
     *
     * @param field AI 字段
     * @param value 选项值
     * @return 配色序号；不配色或值已不在选项里时为空
     */
    public static Optional<Integer> colorIndex(NewsAiFieldPO field, String value) {
        if (!isTagField(field) || field.getKind() == NewsAiFieldKind.BUILTIN) {
            return Optional.empty();
        }
        return Optional.of(options(field).indexOf(value)).filter(index -> index >= 0);
    }

    /**
     * 值是否为空：JSON null、空白文本、空数组。
     *
     * @param value 字段值
     * @return 为空返回 true
     */
    public static boolean isEmptyValue(JsonNode value) {
        if (value.isNull()) {
            return true;
        }
        if (value.isTextual()) {
            return StringUtils.isBlank(value.asText());
        }
        if (value.isArray()) {
            return value.size() == 0;
        }
        return false;
    }

    /**
     * @param field 必填字段
     * @return 必填未填时的提示，如「请填写摘要」「请选择主题」
     */
    public static String requiredMessage(NewsAiFieldPO field) {
        return (isChoice(field) ? "请选择" : "请填写") + field.getName();
    }
}
