package com.zengame.ai.business.news.channel.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索来源的一条结果，各渠道填充同一展示结构，前端不按渠道区分。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceCandidate {

    /**
     * 插件内的来源标识，订阅后存为 news_feed.source_key：公众号微信读书编号、B站 mid、X 用户 rest_id、AIBase 固定值
     */
    private String sourceKey;

    /**
     * 来源名称，即「使用默认名称」填入的值
     */
    private String name;

    /**
     * 来源在渠道上的可读标识，如微信号、UID、@用户名；没有时为空
     */
    private String handle;

    /**
     * 头像地址；没有时为空
     */
    private String avatarUrl;

    /**
     * 简介；没有时为空
     */
    private String description;

    /**
     * 来源的统计项，如粉丝数；没有时为空列表
     */
    private List<StatItem> stats;
}
