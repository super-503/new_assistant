package com.zengame.ai.business.news.review.domain.vo;

import com.zengame.ai.business.news.domain.enums.NewsAiFieldKind;
import lombok.Data;

import java.util.List;

/**
 * 资讯卡片第一行的一组标签：一个标签字段的当前值。
 */
@Data
public class NewsItemTagVO {

    private String fieldKey;

    /**
     * 字段名称
     */
    private String name;

    /**
     * 字段种类，BUILTIN 为岗位分类
     */
    private NewsAiFieldKind kind;

    /**
     * 字段当前值
     */
    private List<NewsFieldOptionVO> values;
}
