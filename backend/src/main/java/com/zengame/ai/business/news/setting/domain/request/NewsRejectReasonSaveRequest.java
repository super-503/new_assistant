package com.zengame.ai.business.news.setting.domain.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 新增或编辑拒绝理由。
 */
@Data
public class NewsRejectReasonSaveRequest {

    @NotBlank(message = "理由名称不能为空")
    @Size(max = 64, message = "理由名称不能超过 64 个字符")
    private String name;

    @NotNull(message = "启用不能为空")
    private Boolean enabled;
}
