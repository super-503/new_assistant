package com.zengame.ai.business.news.setting.domain.bo;

import com.zengame.ai.business.news.setting.domain.enums.NewsDigestDeliveryChannel;
import com.zengame.ai.business.news.setting.domain.enums.NewsPushPlanCard;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/** 推送计划使用的投递目标、内容和每日时间。 */
@Data
public class NewsPushPlanDefinition {
    @NotBlank
    @Size(max = 80)
    private String name;

    @NotNull
    @Pattern(regexp = "(?:[01][0-9]|2[0-3]):[0-5][0-9]")
    private String sendTime;

    @NotNull
    private NewsDigestDeliveryChannel channel;

    @NotNull
    @Pattern(regexp = "cli_[a-zA-Z0-9_-]{1,124}")
    private String appId;

    @NotNull
    @Size(min = 1, max = 100)
    private List<@NotNull @Pattern(regexp = "ou_[a-zA-Z0-9_-]{1,125}") String> recipientOpenIds;

    @NotNull
    private NewsPushPlanCard card;

    @NotNull
    @Valid
    private NewsDigestContentSettings content;
}
