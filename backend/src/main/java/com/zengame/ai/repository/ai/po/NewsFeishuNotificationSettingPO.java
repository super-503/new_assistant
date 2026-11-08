package com.zengame.ai.repository.ai.po;

import lombok.Data;
import lombok.ToString;

/** 飞书应用机器人通知的全局配置记录。 */
@Data
public class NewsFeishuNotificationSettingPO {
    private Integer id;
    private String appId;
    @ToString.Exclude
    private String appSecret;
    private String openId;
    private boolean enabled;
}
