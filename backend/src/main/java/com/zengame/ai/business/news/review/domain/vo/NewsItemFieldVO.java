package com.zengame.ai.business.news.review.domain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldKind;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import lombok.Data;

import java.util.List;

/**
 * 资讯详情里的一个 AI 字段：渲染控件所需的配置和这条资讯上的当前值。
 */
@Data
public class NewsItemFieldVO {

    private String fieldKey;

    /**
     * 字段名称
     */
    private String name;

    private NewsAiFieldKind kind;

    private NewsAiFieldOutputType outputType;

    /**
     * 文本是否多行
     */
    private Boolean multiline;

    /**
     * 单选、多选的选项，按配置顺序；其他类型为空数组
     */
    private List<NewsFieldOptionVO> options;

    private Boolean required;

    /**
     * 是否允许人工修改
     */
    private Boolean editable;

    private Boolean showAsTag;

    /**
     * 当前值：文本、单选为字符串，数字为数值，多选为字符串数组；未生成时为 null
     */
    private JsonNode value;

    /**
     * 是否人工修改过
     */
    private boolean edited;

    /**
     * 是否未生成
     */
    private boolean missing;
}
