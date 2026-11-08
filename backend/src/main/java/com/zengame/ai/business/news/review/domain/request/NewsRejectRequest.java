package com.zengame.ai.business.news.review.domain.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 拒绝一条资讯。
 */
@Data
public class NewsRejectRequest {

    /**
     * 启用中的拒绝理由
     */
    @NotNull
    private Long reasonId;

    /**
     * 补充说明，选填
     */
    @Size(max = 500)
    private String note;
}
