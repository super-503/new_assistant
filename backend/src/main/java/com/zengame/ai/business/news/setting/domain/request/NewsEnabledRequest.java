package com.zengame.ai.business.news.setting.domain.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 启用或停用一项配置（AI 字段、拒绝理由）。
 */
@Data
public class NewsEnabledRequest {

    @NotNull(message = "启用不能为空")
    private Boolean enabled;
}
