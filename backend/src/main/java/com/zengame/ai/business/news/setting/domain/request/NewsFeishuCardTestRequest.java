package com.zengame.ai.business.news.setting.domain.request;

import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuCardSettings;
import com.zengame.ai.business.news.setting.domain.bo.NewsDigestContentSettings;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** 使用编辑中的卡片向指定应用内的接收人发送测试消息。 */
@Getter
@Setter
public class NewsFeishuCardTestRequest extends NewsFeishuAppRequest {
    @NotNull
    @Size(max = 128)
    @Pattern(regexp = "ou_[a-zA-Z0-9_-]+")
    private String openId;

    @NotNull
    @Valid
    private NewsFeishuCardSettings card;

    @Valid
    private NewsDigestContentSettings content;
}
