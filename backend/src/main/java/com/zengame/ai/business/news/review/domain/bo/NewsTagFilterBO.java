package com.zengame.ai.business.news.review.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * 按一个标签字段筛选资讯：字段值命中任一选中值即保留。
 */
@Data
public class NewsTagFilterBO {

    /**
     * 显示为标签的字段标识
     */
    private String fieldKey;

    /**
     * 选中的值
     */
    private List<String> values;
}
