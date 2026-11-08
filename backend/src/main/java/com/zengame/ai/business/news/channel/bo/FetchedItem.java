package com.zengame.ai.business.news.channel.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 渠道拉取的一条资讯，封面、正文媒体和播放地址均来自资讯所属渠道。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FetchedItem {

    /**
 * 渠道拉取的一条资讯，封面、正文媒体和播放地址均来自资讯所属渠道。
 */
    private String canonicalUrl;

    /**
 * 渠道拉取的一条资讯，封面、正文媒体和播放地址均来自资讯所属渠道。
 */
    private String title;

    /**
 * 渠道拉取的一条资讯，封面、正文媒体和播放地址均来自资讯所属渠道。
 */
    private String bodyHtml;

    /**
 * 渠道拉取的一条资讯，封面、正文媒体和播放地址均来自资讯所属渠道。
 */
    private String coverUrl;

    /**
 * 渠道拉取的一条资讯，封面、正文媒体和播放地址均来自资讯所属渠道。
 */
    private String author;

    /**
 * 渠道拉取的一条资讯，封面、正文媒体和播放地址均来自资讯所属渠道。
 */
    private String authorAvatarUrl;

    /**
 * 渠道拉取的一条资讯，封面、正文媒体和播放地址均来自资讯所属渠道。
 */
    private LocalDateTime publishTime;

    /**
 * 渠道拉取的一条资讯，封面、正文媒体和播放地址均来自资讯所属渠道。
 */
    private LocalDateTime feedTime;

    /**
 * 渠道拉取的一条资讯，封面、正文媒体和播放地址均来自资讯所属渠道。
 */
    private Integer durationSeconds;

    /**
 * 渠道拉取的一条资讯，封面、正文媒体和播放地址均来自资讯所属渠道。
 */
    private Playback playback;

    /**
 * 渠道拉取的一条资讯，封面、正文媒体和播放地址均来自资讯所属渠道。
 */
    private List<StatItem> stats;
}
