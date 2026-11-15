package com.zengame.ai.business.news.setting.domain.bo;

import lombok.ToString;
import lombok.Value;

/** 飞书应用机器人通知的发送配置。 */
@Value
public class NewsFeishuNotificationConfig {
    String appId;
    @ToString.Exclude
    String appSecret;
    String openId;
    String recipientName;
    String recipientAvatarUrl;
    boolean enabled;
}
