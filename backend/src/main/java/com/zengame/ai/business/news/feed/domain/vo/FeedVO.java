package com.zengame.ai.business.news.feed.domain.vo;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.feed.domain.enums.NewsFeedStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订阅源表格的一行，也是编辑订阅源弹窗的回填数据。
 */
@Data
public class FeedVO {

    private Long id;

    /**
     * 插件内的来源标识，编辑弹窗据此调用来源预览展示来源卡片
     */
    private String sourceKey;

    /**
     * 来源在渠道上的名称，即「使用默认名称」填入的值
     */
    private String sourceName;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 来源头像地址；没有时为空
     */
    private String avatarUrl;

    /**
     * 来源简介，表格里显示在名称下方；没有时为空
     */
    private String description;

    /**
     * 内容类型
     */
    private NewsContentType contentType;

    /**
     * 内容类型的中文名：文章 / 视频
     */
    private String contentTypeDesc;

    /**
     * 是否启用订阅
     */
    private Boolean enabled;

    /**
     * 展示状态：OK 正常 / ERROR 拉取失败 / PENDING 待首次拉取 / DISABLED 已停用
     */
    private NewsFeedStatus status;

    /**
     * 展示状态的中文名
     */
    private String statusDesc;

    /**
     * 拉取失败的原因，渠道凭证失效时为「渠道凭证失效」；状态不是拉取失败时为空
     */
    private String error;

    /**
     * 最近一次拉取时间；未拉取过时为空
     */
    private LocalDateTime lastPullTime;

    /**
     * 是否正在拉取；为 true 时禁用立即拉取和重试
     */
    private boolean pulling;
}
