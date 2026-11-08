package com.zengame.ai.repository.ai.po;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsFeedPullStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资讯订阅源（news_feed）：渠道下的一个具体来源，同一渠道内按来源标识唯一。
 */
@Data
public class NewsFeedPO {

    private Long id;

    /**
     * 所属渠道标识
     */
    private String channelCode;

    /**
     * 插件内的来源标识：公众号 fakeid、B站 mid、X 用户 rest_id、AIBase 固定值
     */
    private String sourceKey;

    /**
     * 来源在渠道上的名称
     */
    private String sourceName;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 来源头像地址
     */
    private String avatarUrl;

    /**
     * 来源简介
     */
    private String description;

    /**
     * 内容类型，入库资讯沿用此类型
     */
    private NewsContentType contentType;

    /**
     * 是否启用订阅
     */
    private Boolean enabled;

    /**
     * 最近一次拉取的结果
     */
    private NewsFeedPullStatus pullStatus;

    /**
     * 最近一次拉取失败的原因
     */
    private String pullError;

    /**
     * 最近一次拉取时间
     */
    private LocalDateTime lastPullTime;

    /**
     * 拉取进度：已入库条目中最晚的订阅源出现时间；为空表示未拉取过
     */
    private LocalDateTime cursorTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
