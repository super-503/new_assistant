package com.zengame.ai.repository.ai.po;

import com.zengame.ai.business.news.domain.enums.NewsRunStatus;
import com.zengame.ai.business.news.domain.enums.NewsRunTriggerType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资讯渠道运行记录（news_channel_run）：渠道每次拉取结束时写入一条。
 */
@Data
public class NewsChannelRunPO {

    private Long id;

    /**
     * 渠道标识
     */
    private String channelCode;

    /**
     * 手动触发时拉取的订阅源；定时运行为空
     */
    private Long feedId;

    private NewsRunTriggerType triggerType;

    private NewsRunStatus status;

    /**
     * 新增资讯条数
     */
    private Integer newCount;

    /**
     * 运行详情，如各订阅源的结果和失败原因
     */
    private String detail;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;
}
