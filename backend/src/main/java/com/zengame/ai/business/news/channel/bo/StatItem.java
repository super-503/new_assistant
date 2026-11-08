package com.zengame.ai.business.news.channel.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 渠道返回的一项统计，如「播放 124000」「评论 321」，前端统一格式化数值。
 * 资讯的统计项以 JSON 数组存于 news_item.stats_json。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatItem {

    /**
     * 统计项名称，如播放、评论、点赞、回复、粉丝
     */
    private String name;

    /**
     * 统计数值
     */
    private Long value;
}
