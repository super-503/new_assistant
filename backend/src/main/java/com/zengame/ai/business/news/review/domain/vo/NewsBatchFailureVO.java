package com.zengame.ai.business.news.review.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量操作中失败的一条资讯。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsBatchFailureVO {

    private Long id;

    /**
     * 展示标题；资讯不存在时为空
     */
    private String title;

    /**
     * 失败原因
     */
    private String reason;
}
