package com.zengame.ai.business.news.library.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 资讯库左侧分类树：类型 → 渠道 → 订阅源，每级带数量。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsLibraryTreeVO {

    /**
     * 文章、视频两类，固定顺序
     */
    private List<NewsLibraryTypeNodeVO> types;
}
