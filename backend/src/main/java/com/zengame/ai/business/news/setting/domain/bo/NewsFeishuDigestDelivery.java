package com.zengame.ai.business.news.setting.domain.bo;

import lombok.Value;

import java.util.List;
import java.util.function.Consumer;

/** 一次日报任务的卡片设置和投递目标。 */
@Value
public class NewsFeishuDigestDelivery {
    NewsFeishuCardSettings card;
    NewsDigestContentSettings content;
    List<Target> targets;

    @Value
    public static class Target {
        String appId;
        String openId;
        Consumer<String> sendCard;
    }
}
