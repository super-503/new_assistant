package com.zengame.ai.business.news.feed.domain.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 订阅一个从搜索结果里选中的来源。来源信息原样取自搜索结果。
 */
@Data
public class FeedCreateRequest {

    /**
     * 选中来源的标识，取自搜索结果的 sourceKey
     */
    @NotBlank(message = "请选择来源")
    @Size(max = 128, message = "来源标识过长")
    private String sourceKey;

    /**
     * 来源名称，取自搜索结果的 name
     */
    @NotBlank(message = "来源名称不能为空")
    @Size(max = 128, message = "来源名称过长")
    private String sourceName;

    /**
     * 来源头像地址，取自搜索结果的 avatarUrl；没有时为空
     */
    @Size(max = 1024, message = "来源头像地址过长")
    private String avatarUrl;

    /**
     * 来源简介，取自搜索结果的 description；没有时为空
     */
    @Size(max = 1000, message = "来源简介过长")
    private String description;

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
