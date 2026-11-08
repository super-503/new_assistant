package com.zengame.ai.repository.ai.po;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资讯（news_item）：订阅源拉取到的一条内容，按规范化的原文链接全局唯一。
 */
@Data
public class NewsItemPO {

    private Long id;

    /**
     * 来源订阅源
     */
    private Long feedId;

    /**
     * 所属渠道标识
     */
    private String channelCode;

    /**
     * 规范化后的原文链接
     */
    private String canonicalUrl;

    /**
     * 内容类型，取自订阅源
     */
    private NewsContentType contentType;

    /**
     * 原文标题；X 推文没有标题
     */
    private String originTitle;

    private String author;

    private String authorAvatarUrl;

    private LocalDateTime publishTime;

    /**
     * 封面地址
     */
    private String coverUrl;

    /**
     * 清洗后的正文 HTML；为空表示正文暂不可获取
     */
    private String bodyHtml;

    /**
     * 站内播放方式 JSON，结构为 {@code Playback}；没有时为空
     */
    private String playbackJson;

    /**
     * 统计项 JSON 数组，元素结构为 {@code StatItem}
     */
    private String statsJson;

    /**
     * 视频时长（秒），视频才有
     */
    private Integer durationSeconds;

    private NewsItemStatus status;

    /**
     * AI 字段结果 JSON 对象：{字段标识: 值}
     */
    private String aiValuesJson;

    /**
     * 未生成的字段标识 JSON 数组
     */
    private String aiFailedKeysJson;

    /**
     * 人工修改过的字段标识 JSON 数组
     */
    private String editedKeysJson;

    /**
     * 拒绝理由
     */
    private Long rejectReasonId;

    /**
     * 拒绝补充说明
     */
    private String rejectNote;

    /**
     * 审核人登录名
     */
    private String reviewer;

    private LocalDateTime reviewTime;

    /**
     * 个人日报发送成功的时间；未发送时为空
     */
    private LocalDateTime digestSentTime;

    /**
     * 入库时间
     */
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
