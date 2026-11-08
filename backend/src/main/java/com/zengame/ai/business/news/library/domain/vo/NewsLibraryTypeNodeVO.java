package com.zengame.ai.business.news.library.domain.vo;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分类树里的一个类型：文章或视频。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsLibraryTypeNodeVO {

    private NewsContentType contentType;

    /**
     * 类型名：文章 / 视频
     */
    private String name;

    /**
     * 这一类型已通过的资讯总数
     */
    private long count;

    /**
     * 有资讯的渠道，按渠道主键升序
     */
    private List<NewsLibraryChannelNodeVO> channels;
}
