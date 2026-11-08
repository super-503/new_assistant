package com.zengame.ai.business.news.channel.bo;

import com.zengame.ai.business.news.domain.enums.NewsPlaybackKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频的站内播放方式，以 JSON 存于 news_item.playback_json。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Playback {

    /**
     * 播放方式
     */
    private NewsPlaybackKind kind;

    /**
     * EMBED 为内嵌播放器地址，主机名必须在插件声明的 embedDomains 内；
     * FILE 为渠道提供的视频文件地址
     */
    private String url;

    /**
     * FILE 的视频封面取渠道原始地址，EMBED 为空
     */
    private String coverUrl;
}
