package com.zengame.ai.business.news.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 字段种类（news_ai_field.kind），决定字段在基础配置里可修改的范围。
 */
@Getter
@AllArgsConstructor
public enum NewsAiFieldKind implements CodeEnum {

    /** 系统字段（标题）：不能删除，字段标识和输出类型不能改，其余可以编辑 */
    SYSTEM("系统"),

    /** 内置字段（岗位分类）：只能改提示词，选项由职位族同步，不能删除 */
    BUILTIN("内置"),

    /** 普通字段：全部可改，可以删除 */
    CUSTOM("普通");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的字段种类
     */
    @JsonCreator
    public static NewsAiFieldKind fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsAiFieldKind.class);
    }
}
