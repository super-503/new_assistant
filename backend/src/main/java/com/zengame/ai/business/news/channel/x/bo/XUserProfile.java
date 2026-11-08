package com.zengame.ai.business.news.channel.x.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * X 用户资料，来自 UserByScreenName / UserByRestId。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XUserProfile {

    /**
     * 用户 rest_id，订阅后存为 news_feed.source_key
     */
    private String restId;

    /**
     * 显示名称
     */
    private String name;

    /**
     * 不带 @ 的用户名
     */
    private String screenName;

    /**
     * 原尺寸头像地址
     */
    private String avatarUrl;

    /**
     * 简介，t.co 短链已换成原始链接；没有时为空
     */
    private String description;

    /**
     * 关注者数
     */
    private Long followersCount;
}
