package com.zengame.ai.business.news.feed.domain.request;

import lombok.Data;

/**
 * 在渠道内搜索来源。
 */
@Data
public class SourceSearchRequest {

    /**
     * 关键词，如公众号名称、B站昵称或 UID、X 的 @用户名；支持搜索的渠道必填，不支持搜索的渠道不传
     */
    private String keyword;
}
