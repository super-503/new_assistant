package com.zengame.ai.business.news.review.domain.vo;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资讯列表项，审核队列和资讯库共用。
 */
@Data
public class NewsItemCardVO {

    private Long id;

    /**
     * 展示标题：AI 字段「标题」的结果，没有时取原文标题
     */
    private String title;

    /**
     * 摘要：文章取 AI 字段「摘要」，视频取正文纯文本前 120 字作为视频简介；没有时为空
     */
    private String summary;

    private NewsContentType contentType;

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

    private LocalDateTime publishTime;

    /**
     * 缩略图
     */
    private String coverUrl;

    /**
     * 视频时长（秒）
     */
    private Integer durationSeconds;

    /**
     * 原文链接
     */
    private String url;

    private NewsItemStatus status;

    /**
     * 标签：先是普通标签字段，再是岗位分类；没有值的字段不出现
     */
    private List<NewsItemTagVO> tags;

    /**
     * 未生成的字段，按展示顺序；不为空时卡片显示结构化失败提示
     */
    private List<NewsFieldRefVO> missingFields;

    /**
     * 拒绝理由名称；未拒绝时为空
     */
    private String rejectReasonName;

    /**
     * 拒绝补充说明
     */
    private String rejectNote;

    /**
     * 审核人登录名
     */
    private String reviewer;

    private LocalDateTime reviewTime;
}
