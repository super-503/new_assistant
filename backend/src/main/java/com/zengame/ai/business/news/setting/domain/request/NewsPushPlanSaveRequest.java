package com.zengame.ai.business.news.setting.domain.request;

import com.zengame.ai.business.news.setting.domain.bo.NewsPushPlanDefinition;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

/** 管理员编辑的推送计划及并发版本。 */
@Data
public class NewsPushPlanSaveRequest {
    @PositiveOrZero
    private Long revision;

    private boolean enabled;

    @NotNull
    @Valid
    private NewsPushPlanDefinition definition;
}
