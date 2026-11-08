package com.zengame.ai.news;

import com.zengame.ai.business.news.domain.enums.NewsAiFieldKind;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldSource;
import com.zengame.ai.business.news.processing.StringListJson;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;

import java.util.List;

/**
 * 数据加工纯逻辑测试用的 AI 字段构造。
 */
final class NewsAiFieldFixtures {

    private NewsAiFieldFixtures() {
    }

    static NewsAiFieldPO field(String key, NewsAiFieldSource source, NewsAiFieldOutputType outputType, List<String> options) {
        NewsAiFieldPO field = new NewsAiFieldPO();
        field.setFieldKey(key);
        field.setName(key + "名称");
        field.setKind(NewsAiFieldKind.CUSTOM);
        field.setSource(source);
        field.setOutputType(outputType);
        field.setMultiline(false);
        field.setOptionsJson(StringListJson.write(options));
        field.setShowAsTag(false);
        field.setPrompt(key + "的提示词");
        field.setRequired(false);
        field.setEditable(true);
        field.setEnabled(true);
        field.setArticleOnly(false);
        field.setSortNo(1);
        return field;
    }
}
