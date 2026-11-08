package com.zengame.ai.business.news.setting.domain.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** 飞书自建应用凭证。密钥留空时沿用同一应用的已保存值。 */
@Getter
@Setter
public class NewsFeishuAppRequest {

    @NotNull
    @Pattern(regexp = "cli_[a-zA-Z0-9_-]+", message = "请填写有效的飞书 App ID")
    @Size(max = 128)
    private String appId;

    @Size(max = 512)
    @Pattern(regexp = "[^\\p{Cntrl}]*", message = "App Secret 不能包含控制字符")
    private String appSecret;
}
