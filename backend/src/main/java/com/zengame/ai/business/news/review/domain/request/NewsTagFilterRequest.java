package com.zengame.ai.business.news.review.domain.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 一个标签字段的筛选：字段值命中任一选中值即保留。
 */
@Data
public class NewsTagFilterRequest {

    /**
     * 启用中的标签字段标识
     */
    @NotBlank
    private String fieldKey;

    /**
     * 选中的值
     */
    @NotEmpty
    private List<String> values;
}
