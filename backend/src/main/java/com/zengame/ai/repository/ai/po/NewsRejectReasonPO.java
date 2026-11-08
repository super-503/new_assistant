package com.zengame.ai.repository.ai.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资讯拒绝理由（news_reject_reason）：拒绝资讯时选择的预设理由。
 */
@Data
public class NewsRejectReasonPO {

    private Long id;

    /**
     * 理由名称
     */
    private String name;

    private Boolean enabled;

    /**
     * 排序，越小越靠前
     */
    private Integer sortNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
