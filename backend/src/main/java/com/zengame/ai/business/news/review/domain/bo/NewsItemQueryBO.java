package com.zengame.ai.business.news.review.domain.bo;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资讯列表的查询条件，审核队列和资讯库共用。为空的条件表示不限。
 */
@Data
public class NewsItemQueryBO {

    private NewsItemStatus status;

    private NewsContentType contentType;

    private String channelCode;

    private Long feedId;

    /**
     * 匹配展示标题
     */
    private String keyword;

    /** 允许按 AI 标题搜索。 */
    private boolean useAiTitle;

    /**
     * 发布时间下界（含）
     */
    private LocalDateTime publishBegin;

    /**
     * 发布时间上界（含）
     */
    private LocalDateTime publishEnd;

    /**
     * 各标签字段的筛选，字段之间同时满足
     */
    private List<NewsTagFilterBO> tagFilters;

    /**
     * true 按发布时间从旧到新，false 从新到旧
     */
    private boolean oldestFirst;
}
