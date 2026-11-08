package com.zengame.ai.business.news.feed.domain.vo;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import lombok.Data;

import java.util.List;

/**
 * 选中来源后展示的来源卡片。
 */
@Data
public class SourcePreviewVO {

    /**
     * 来源信息，与搜索结果同一结构
     */
    private SourceCandidateVO source;

    /**
     * 来源的内容类型标签，也是订阅时默认选中的类型
     */
    private NewsContentType contentType;

    /**
     * 内容类型的中文名：文章 / 视频
     */
    private String contentTypeDesc;

    /**
     * 最近更新，按发布时间倒序；没有时为空列表
     */
    private List<SourceRecentItemVO> recentItems;
}
