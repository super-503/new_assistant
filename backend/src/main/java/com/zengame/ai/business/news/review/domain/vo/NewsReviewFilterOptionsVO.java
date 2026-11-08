package com.zengame.ai.business.news.review.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 审核队列筛选栏的可选项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsReviewFilterOptionsVO {

    /**
     * 全部渠道及其订阅源，按主键升序
     */
    private List<NewsChannelOptionVO> channels;

    /**
     * 启用中的标签字段：先是普通标签字段，再是岗位分类
     */
    private List<NewsTagFieldVO> tagFields;
}
