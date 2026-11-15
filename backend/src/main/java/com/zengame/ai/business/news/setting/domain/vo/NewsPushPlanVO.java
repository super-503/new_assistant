package com.zengame.ai.business.news.setting.domain.vo;

import com.zengame.ai.business.news.setting.domain.bo.NewsPushPlanDefinition;
import lombok.Value;

/** 管理端可编辑的推送计划。 */
@Value
public class NewsPushPlanVO {
    long id;
    long revision;
    boolean enabled;
    NewsPushPlanDefinition definition;
}
