package com.zengame.ai.business.news.review.domain.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 对多条资讯逐条执行的操作。
 */
@Data
public class NewsItemIdsRequest {

    @NotEmpty
    private List<Long> ids;
}
