package com.zengame.ai.business.news.setting.domain.vo;

import lombok.Data;

/**
 * 拒绝理由配置。
 */
@Data
public class NewsRejectReasonVO {

    private Long id;

    private String name;

    private Boolean enabled;

    private Integer sortNo;
}
