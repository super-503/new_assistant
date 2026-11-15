package com.zengame.ai.business.news.setting.domain.vo;

import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuCardSettings;
import com.zengame.ai.business.news.setting.domain.bo.NewsDigestContentSettings;
import lombok.Value;

import java.util.List;

/** 管理端可查看的飞书渠道配置。 */
@Value
public class NewsFeishuChannelVO {
    long revision;
    List<App> apps;
    List<Recipient> recipients;
    NewsFeishuCardSettings card;
    NewsDigestContentSettings content;
    boolean enabled;

    @Value
    public static class App {
        String appId;
        String name;
        String avatarUrl;
    }

    @Value
    public static class Recipient {
        String appId;
        String openId;
        String name;
        String avatarUrl;
    }
}
