package com.zengame.ai.business.news.review.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单选、多选字段的一个取值及其标签配色。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsFieldOptionVO {

    private String value;

    /**
     * 标签配色序号：值在字段选项里的下标，前端按 5 组配色循环取用。
     * 岗位分类、非标签字段、已不在选项里的值为空，前端用中性样式
     */
    private Integer colorIndex;
}
