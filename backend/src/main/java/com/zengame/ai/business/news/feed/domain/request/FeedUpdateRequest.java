package com.zengame.ai.business.news.feed.domain.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 编辑订阅源：只能改显示名称、内容类型、是否启用，渠道和来源不可修改。
 */
@Data
public class FeedUpdateRequest {

    /**
     * 显示名称
     */
    @NotBlank(message = "请填写显示名称")
    @Size(max = 128, message = "显示名称不能超过 128 个字")
    private String displayName;

    /**
     * 内容类型编码：ARTICLE / VIDEO
     */
    @NotBlank(message = "请选择内容类型")
    private String contentType;

    /**
     * 是否启用订阅
     */
    @NotNull(message = "请设置是否启用订阅")
    private Boolean enabled;
}
