package com.zengame.ai.business.news.review.domain.vo;

import com.zengame.ai.business.news.domain.enums.NewsAiFieldKind;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import lombok.Data;

import java.util.List;

/**
 * 可用于筛选的标签字段及其选项。
 */
@Data
public class NewsTagFieldVO {

    private String fieldKey;

    /**
     * 字段名称，即筛选 chip 未选中时的文字
     */
    private String name;

    /**
     * 字段种类，BUILTIN 为岗位分类
     */
    private NewsAiFieldKind kind;

    private NewsAiFieldOutputType outputType;

    /**
     * 选项，按配置顺序
     */
    private List<NewsFieldOptionVO> options;
}
