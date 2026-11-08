package com.zengame.ai.business.news.review.domain.request;

import com.zengame.ai.infrastructure.common.BasePageInfo;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 审核队列分页请求。查询条件和分页参数分开承载。
 */
@Data
public class NewsReviewPageQueryRequest {

    @Valid
    @NotNull
    private NewsReviewPageRequest query;

    private BasePageInfo basePageInfo = new BasePageInfo();
}
