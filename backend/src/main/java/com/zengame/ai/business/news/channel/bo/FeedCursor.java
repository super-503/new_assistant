package com.zengame.ai.business.news.channel.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 一次拉取的输入：拉取哪个来源、从什么时间之后开始。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedCursor {

    /**
     * 插件内的来源标识（news_feed.source_key）
     */
    private String sourceKey;

    /**
     * 只取订阅源出现时间晚于此刻的内容：取 news_feed.cursor_time，首次拉取为当前时间往前 7 天
     */
    private LocalDateTime since;
}
