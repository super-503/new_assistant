package com.zengame.ai.business.news.library.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类树里的订阅源。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsLibraryFeedNodeVO {

    private Long feedId;

    /**
     * 订阅源显示名称
     */
    private String feedName;

    private String avatarUrl;

    /**
     * 该订阅源下已通过的资讯条数
     */
    private long count;
}
