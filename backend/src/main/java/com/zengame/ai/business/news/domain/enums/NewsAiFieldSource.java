package com.zengame.ai.business.news.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 字段的来源内容（news_ai_field.source）：生成该字段时交给模型的资讯内容。
 */
@Getter
@AllArgsConstructor
public enum NewsAiFieldSource implements CodeEnum {

    BODY("正文"),

    TITLE("标题"),

    TITLE_BODY("标题 + 正文");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的来源内容
     */
    @JsonCreator
    public static NewsAiFieldSource fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsAiFieldSource.class);
    }
}
