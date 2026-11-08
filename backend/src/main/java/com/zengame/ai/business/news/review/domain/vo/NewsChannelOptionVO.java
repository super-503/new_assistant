package com.zengame.ai.business.news.review.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 筛选里的一个渠道及其订阅源。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsChannelOptionVO {

    private String channelCode;

    private String channelName;

    /**
     * 渠道下现存的订阅源，按主键升序
     */
    private List<NewsFeedOptionVO> feeds;
}
