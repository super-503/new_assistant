package com.zengame.ai.business.news.review.domain.vo;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import lombok.Data;

/**
 * 筛选里的一个订阅源。
 */
@Data
public class NewsFeedOptionVO {

    private Long feedId;

    /**
     * 订阅源显示名称
     */
    private String feedName;

    private String avatarUrl;

    private NewsContentType contentType;
}
