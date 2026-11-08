package com.zengame.ai.business.news.channel.bo;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 选中来源后展示的来源卡片：来源信息、内容类型和最近更新。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourcePreview {

    /**
     * 来源信息，与搜索结果同一结构
     */
    private SourceCandidate source;

    /**
     * 来源的内容类型标签，也是订阅时默认选中的类型
     */
    private NewsContentType contentType;

    /**
     * 最近更新，按发布时间倒序；没有时为空列表
     */
    private List<SourceRecentItem> recentItems;
}
