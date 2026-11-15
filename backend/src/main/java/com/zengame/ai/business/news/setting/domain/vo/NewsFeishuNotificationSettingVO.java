package com.zengame.ai.business.news.setting.domain.vo;

import lombok.Value;

/** 管理员可查看的飞书通知配置状态。 */
@Value
public class NewsFeishuNotificationSettingVO {
    String appId;
    String openId;
    String recipientName;
    String recipientAvatarUrl;
    boolean configured;
    boolean enabled;
}
