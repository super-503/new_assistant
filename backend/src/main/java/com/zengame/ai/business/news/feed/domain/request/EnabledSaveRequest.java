package com.zengame.ai.business.news.feed.domain.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 切换启用状态：订阅源行内的启用开关、渠道设置里的启用渠道。
 */
@Data
public class EnabledSaveRequest {

    /**
     * 是否启用
     */
    @NotNull(message = "请设置是否启用")
    private Boolean enabled;
}
