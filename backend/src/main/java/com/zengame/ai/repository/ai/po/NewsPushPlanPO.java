package com.zengame.ai.repository.ai.po;

import lombok.Data;

import java.time.LocalDateTime;

/** 推送计划、编辑版本和下一次运行时间。 */
@Data
public class NewsPushPlanPO {
    private Long id;
    private long revision;
    private boolean enabled;
    private LocalDateTime nextRunAt;
    private String configJson;
}
