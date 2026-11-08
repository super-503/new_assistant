package com.zengame.ai.business.news.review.domain.vo;

import com.zengame.ai.business.news.channel.bo.Playback;
import com.zengame.ai.business.news.channel.bo.StatItem;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资讯详情，审核详情面板和资讯库整页详情共用。
 */
@Data
public class NewsItemDetailVO {

    private Long id;

    /**
     * 展示标题：AI 字段「标题」的结果，没有时取原文标题
     */
    private String title;

    /**
     * 原文标题
     */
    private String originTitle;

    /**
     * 摘要：文章取 AI 字段「摘要」，视频取正文纯文本前 120 字作为视频简介；没有时为空
     */
    private String summary;

    private NewsContentType contentType;

    /**
     * 类型名：文章 / 视频
     */
    private String contentTypeName;

    private String channelCode;

    private String channelName;

    private Long feedId;

    /**
     * 订阅源显示名称；订阅源已删除时为空
     */
    private String feedName;

    /**
     * 来源头像（订阅源头像）；订阅源已删除或没有头像时为空
     */
    private String feedAvatarUrl;

    private String author;

    private String authorAvatarUrl;

    private LocalDateTime publishTime;

    private String coverUrl;

    private Integer durationSeconds;

    /**
     * 原文链接
     */
    private String url;

    private NewsItemStatus status;

    /**
     * 状态名，如待审、已拒绝、已通过
     */
    private String statusName;

    /**
     * 清洗后的正文 HTML；为空表示正文暂不可获取
     */
    private String bodyHtml;

    /**
     * 站内播放方式；没有时为空
     */
    private Playback playback;

    /**
     * 统计项
     */
    private List<StatItem> stats;

    /**
     * 标签，结构同列表项
     */
    private List<NewsItemTagVO> tags;

    /**
     * 未生成的字段，按展示顺序
     */
    private List<NewsFieldRefVO> missingFields;

    /**
     * 对这条资讯适用的启用 AI 字段，按展示顺序
     */
    private List<NewsItemFieldVO> fields;

    private String rejectReasonName;

    private String rejectNote;

    private String reviewer;

    private LocalDateTime reviewTime;

    /**
     * 处理记录，最近的在前
     */
    private List<NewsItemLogVO> logs;
}
