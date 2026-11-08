package com.zengame.ai.business.news.feed.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 来源卡片「最近更新」列表里的一条内容。
 */
@Data
public class SourceRecentItemVO {

    /**
     * 标题
     */
    private String title;

    /**
     * 缩略图地址；没有时为空
     */
    private String thumbnailUrl;

    /**
     * 发布时间，前端精确到分钟显示
     */
    private LocalDateTime publishTime;

    /**
     * 视频时长（秒），视频才有，显示为缩略图上的时长角标
     */
    private Integer durationSeconds;

    /**
     * 渠道返回的统计项；没有时为空列表
     */
    private List<StatItemVO> stats;
}
