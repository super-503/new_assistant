package com.zengame.ai.business.paper.domain.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 单个来源的论文关键词和分页请求。 */
@Data
public class PaperSearchRequest {

    @NotBlank
    @Size(max = 100)
    private String keyword;

    @NotBlank
    private String source;

    @Size(max = 512)
    private String cursor;
}
