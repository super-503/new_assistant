package com.zengame.ai.business.news.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 字段的输出类型（news_ai_field.output_type），决定生成约束和审核时的控件。
 */
@Getter
@AllArgsConstructor
public enum NewsAiFieldOutputType implements CodeEnum {

    /** 文本，可设为多行 */
    TEXT("文本"),

    NUMBER("数字"),

    /** 单选，只能在选项范围内取一个 */
    SINGLE("单选"),

    /** 多选，只能在选项范围内取若干个 */
    MULTI("多选");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的输出类型
     */
    @JsonCreator
    public static NewsAiFieldOutputType fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsAiFieldOutputType.class);
    }
}
