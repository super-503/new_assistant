package com.zengame.ai.business.news.feed.domain.vo;

import com.zengame.ai.business.news.domain.enums.NewsRunStatus;
import com.zengame.ai.business.news.domain.enums.NewsRunTriggerType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运行记录侧边栏里的一次渠道运行。
 */
@Data
public class ChannelRunVO {

    private Long id;

    /**
     * 触发方式：SCHEDULE 定时 / MANUAL 手动
     */
    private NewsRunTriggerType triggerType;

    /**
     * 触发方式的中文名
     */
    private String triggerTypeDesc;

    /**
     * 结果：SUCCEEDED 成功 / FAILED 失败
     */
    private NewsRunStatus status;

    /**
     * 结果的中文名
     */
    private String statusDesc;

    /**
     * 新增资讯条数
     */
    private Integer newCount;

    /**
     * 运行详情，展开后显示
     */
    private String detail;

    /**
     * 手动触发时拉取的订阅源；定时运行为空
     */
    private Long feedId;

    /**
     * 手动触发时拉取的订阅源显示名称；定时运行或订阅源已删除时为空
     */
    private String feedName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
