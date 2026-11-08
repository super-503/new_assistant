package com.zengame.ai.business.news.library.domain.request;

import com.zengame.ai.business.news.review.domain.request.NewsTagFilterRequest;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

/**
 * 资讯库列表的查询条件。为空的条件表示不限。
 */
@Data
public class NewsLibraryPageRequest {

    /**
     * 类型：ARTICLE / VIDEO
     */
    @NotBlank
    private String contentType;

    /**
     * 渠道标识，查看整个渠道时传
     */
    private String channelCode;

    /**
     * 订阅源，查看单个订阅源时传
     */
    private Long feedId;

    /**
     * 关键词，匹配展示标题
     */
    private String keyword;

    /**
     * 发布日期区间 [起始日, 结束日]，格式 yyyy-MM-dd，两端都包含
     */
    private List<LocalDate> publishPeriod;

    /**
     * 各标签字段的筛选，字段之间同时满足
     */
    @Valid
    private List<NewsTagFilterRequest> tagFilters;
}
