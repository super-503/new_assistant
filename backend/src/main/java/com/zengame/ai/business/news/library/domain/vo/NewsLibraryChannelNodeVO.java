package com.zengame.ai.business.news.library.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分类树里的渠道分组。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsLibraryChannelNodeVO {

    private String channelCode;

    private String channelName;

    /**
     * 该渠道下这一类型已通过的资讯条数，含已删除订阅源的资讯
     */
    private long count;

    /**
     * 现存的订阅源，按主键升序；已删除订阅源的资讯不出现在这一级
     */
    private List<NewsLibraryFeedNodeVO> feeds;
}
