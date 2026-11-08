package com.zengame.ai.business.news.feed.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 搜索来源的一条结果，各渠道同一展示结构。
 */
@Data
public class SourceCandidateVO {

    /**
     * 插件内的来源标识，预览和订阅时回传
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
    private List<StatItemVO> stats;
}
