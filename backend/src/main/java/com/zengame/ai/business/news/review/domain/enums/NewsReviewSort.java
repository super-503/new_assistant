package com.zengame.ai.business.news.review.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审核队列的排序方式，按资讯发布时间排列。
 */
@Getter
@AllArgsConstructor
public enum NewsReviewSort implements CodeEnum {

    /** 发布时间从新到旧 */
    NEWEST("最新"),

    /** 发布时间从旧到新 */
    OLDEST("最早");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的排序方式
     */
    @JsonCreator
    public static NewsReviewSort fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsReviewSort.class);
    }
}
