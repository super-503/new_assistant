package com.zengame.ai.repository.ai.po;

import com.zengame.ai.business.news.domain.enums.NewsAiFieldKind;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldSource;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资讯 AI 字段（news_ai_field）：数据加工结构化出哪些字段，审核和资讯库展示哪些字段。
 */
@Data
public class NewsAiFieldPO {

    private Long id;

    /**
     * 字段标识，结构化结果按它存入 news_item.ai_values_json
     */
    private String fieldKey;

    /**
     * 字段名称
     */
    private String name;

    private NewsAiFieldKind kind;

    /**
     * 生成时交给模型的资讯内容
     */
    private NewsAiFieldSource source;

    private NewsAiFieldOutputType outputType;

    /**
     * 文本是否多行
     */
    private Boolean multiline;

    /**
     * 单选、多选的选项 JSON 字符串数组
     */
    private String optionsJson;

    /**
     * 是否显示为标签
     */
    private Boolean showAsTag;

    private String prompt;

    /**
     * 是否必填：不填不能保存，也不能通过
     */
    private Boolean required;

    /**
     * 是否允许人工修改
     */
    private Boolean editable;

    private Boolean enabled;

    /**
     * 是否只对文章生成
     */
    private Boolean articleOnly;

    /**
     * 排序，越小越靠前
     */
    private Integer sortNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
