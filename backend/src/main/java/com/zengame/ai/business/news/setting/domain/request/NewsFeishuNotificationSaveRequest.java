package com.zengame.ai.business.news.setting.domain.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** 飞书通知配置表单。同一个 App ID 的密钥留空时沿用已保存的值。 */
@Getter
@Setter
public class NewsFeishuNotificationSaveRequest extends NewsFeishuAppRequest {

    @NotNull
    @Pattern(regexp = "ou_[a-zA-Z0-9_-]+", message = "请填写有效的飞书接收人 Open ID")
    @Size(max = 128)
    private String openId;

    @Size(max = 255)
    private String recipientName;

    @Size(max = 2048)
    @Pattern(regexp = "https://[^\\s]+", message = "飞书头像必须使用 HTTPS 地址")
    private String recipientAvatarUrl;

    @NotNull
    private Boolean enabled;
}
