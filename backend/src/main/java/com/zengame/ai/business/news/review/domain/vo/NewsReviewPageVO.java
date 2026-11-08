package com.zengame.ai.business.news.review.domain.vo;

import com.zengame.ai.infrastructure.common.BasePageInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 审核队列的一页，附带两个分类的总数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsReviewPageVO {

    /**
     * 待审总数，不受筛选影响
     */
    private long pendingCount;

    /**
     * 已拒绝总数，不受筛选影响
     */
    private long rejectedCount;

    private List<NewsItemCardVO> list;

    /**
     * 分页参数，total 为当前分类在筛选后的条数
     */
    private BasePageInfo pagination;
}
