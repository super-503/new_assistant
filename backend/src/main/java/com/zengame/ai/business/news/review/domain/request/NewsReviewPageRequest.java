package com.zengame.ai.business.news.review.domain.request;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

/**
 * 审核队列的查询条件。为空的条件表示不限。
 */
@Data
public class NewsReviewPageRequest {

    /**
     * 分类：PENDING 待审 / REJECTED 已拒绝
     */
    @NotBlank
    private String status;

    /**
     * 类型：ARTICLE / VIDEO
     */
    private String contentType;

    private String channelCode;

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

    /**
     * 排序：NEWEST 最新 / OLDEST 最早，按发布时间
     */
    @NotBlank
    private String sort = "NEWEST";
}
