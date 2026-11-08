package com.zengame.ai.business.news.feed.domain.vo;

import lombok.Data;

/**
 * 「需处理」里拉取失败的一个订阅源。
 */
@Data
public class TodoFeedVO {

    /**
     * 所属渠道标识，重试、编辑和跳转渠道时回传
     */
    private String channelCode;

    /**
     * 所属渠道名称
     */
    private String channelName;

    /**
     * 订阅源，失败原因见 {@link FeedVO#getError()}
     */
    private FeedVO feed;
}
