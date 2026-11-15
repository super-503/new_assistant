package com.zengame.ai.business.news.setting.domain.vo;

import lombok.Value;

import java.util.List;

/** 推送计划列表及每日发送时间所用时区。 */
@Value
public class NewsPushPlanListVO {
    String timeZone;
    List<NewsPushPlanVO> plans;
}
