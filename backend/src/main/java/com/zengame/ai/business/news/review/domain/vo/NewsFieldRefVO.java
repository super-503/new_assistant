package com.zengame.ai.business.news.review.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段的标识和名称。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsFieldRefVO {

    private String fieldKey;

    private String name;
}
