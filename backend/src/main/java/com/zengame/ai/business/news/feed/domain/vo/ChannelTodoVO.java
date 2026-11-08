package com.zengame.ai.business.news.feed.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 渠道配置页「需处理」汇总。两部分都为空时显示「暂无需处理事项」。
 */
@Data
public class ChannelTodoVO {

    /**
     * 待处理事项总数，即凭证失效的渠道数加拉取失败的订阅源数，显示在「需处理」入口上
     */
    private int total;

    /**
     * 凭证已失效的渠道，每行带「更新凭证」
     */
    private List<ChannelVO> credentialChannels;

    /**
     * 拉取失败的订阅源，每行带原因、「重试」「编辑」；凭证已失效的渠道下的订阅源不在其中
     */
    private List<TodoFeedVO> failedFeeds;
}
