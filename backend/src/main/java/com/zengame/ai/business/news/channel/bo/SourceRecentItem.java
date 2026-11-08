package com.zengame.ai.business.news.channel.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 来源预览「最近更新」列表里的一条内容。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceRecentItem {

    /**
     * 标题；X 推文取正文开头
     */
    private String title;

    /**
     * 缩略图原始地址；没有时为空
     */
    private String thumbnailUrl;

    /**
     * 发布时间，前端精确到分钟显示
     */
    private LocalDateTime publishTime;

    /**
     * 视频时长（秒），视频才有
     */
    private Integer durationSeconds;

    /**
     * 渠道返回的统计项，如 B站播放数和评论数、X 点赞数和回复数；没有时为空列表
     */
    private List<StatItem> stats;
}
