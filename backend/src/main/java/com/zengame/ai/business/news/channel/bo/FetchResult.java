package com.zengame.ai.business.news.channel.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 一次拉取的结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FetchResult {

    /**
     * 订阅源最新一页中，订阅源出现时间晚于 {@link FeedCursor#getSince()} 的条目；没有新内容时为空列表
     */
    private List<FetchedItem> items;
}
