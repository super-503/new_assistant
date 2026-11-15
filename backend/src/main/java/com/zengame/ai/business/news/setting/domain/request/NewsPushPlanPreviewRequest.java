package com.zengame.ai.business.news.setting.domain.request;

import com.zengame.ai.business.news.setting.domain.bo.NewsPushPlanDefinition;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/** 使用计划中的应用、卡片和内容向指定计划接收人试发。 */
@Data
public class NewsPushPlanPreviewRequest {
    @NotNull
    @Valid
    private NewsPushPlanDefinition definition;

    @NotNull
    @Pattern(regexp = "ou_[a-zA-Z0-9_-]{1,125}")
    private String openId;
}
