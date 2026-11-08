package com.zengame.ai.business.news.feed.domain.vo;

import lombok.Data;

/**
 * 渠道返回的一项统计，如「播放 124000」「粉丝 3200」，前端统一格式化数值。
 */
@Data
public class StatItemVO {

    /**
     * 统计项名称，如播放、评论、点赞、回复、粉丝
     */
    private String name;

    /**
     * 统计数值
     */
    private Long value;
}
