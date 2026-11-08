package com.zengame.ai.business.news.feed.domain.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 预览一个来源。
 */
@Data
public class SourcePreviewRequest {

    /**
     * 来源标识，取自搜索结果或订阅源的 sourceKey
     */
    @NotBlank(message = "请选择来源")
    private String sourceKey;
}
