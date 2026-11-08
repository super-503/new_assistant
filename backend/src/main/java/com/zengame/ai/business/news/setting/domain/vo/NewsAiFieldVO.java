package com.zengame.ai.business.news.setting.domain.vo;

import com.zengame.ai.business.news.domain.enums.NewsAiFieldKind;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldSource;
import lombok.Data;

import java.util.List;

/**
 * AI 字段配置，基础配置列表和编辑弹窗使用。
 */
@Data
public class NewsAiFieldVO {

    private Long id;

    private String fieldKey;

    private String name;

    /**
     * 种类：SYSTEM 显示「系统」标签，BUILTIN 显示「内置」标签
     */
    private NewsAiFieldKind kind;

    /**
     * 种类中文名：系统 / 内置 / 普通
     */
    private String kindDesc;

    private NewsAiFieldSource source;

    /**
     * 来源内容中文名：正文 / 标题 / 标题 + 正文
     */
    private String sourceDesc;

    private NewsAiFieldOutputType outputType;

    /**
     * 输出类型的展示名：单行文本 / 多行文本 / 数字 / 单选 / 多选
     */
    private String outputTypeLabel;

    private Boolean multiline;

    /**
     * 单选、多选的选项
     */
    private List<String> options;

    private Boolean showAsTag;

    private String prompt;

    private Boolean required;

    private Boolean editable;

    private Boolean enabled;

    /**
     * 是否只对文章生成
     */
    private Boolean articleOnly;

    private Integer sortNo;

    /**
     * 是否可以删除：只有普通字段可以
     */
    private Boolean deletable;

    /**
     * 字段标识是否锁定：系统字段、内置字段锁定
     */
    private Boolean fieldKeyLocked;

    /**
     * 输出类型是否锁定：系统字段、内置字段锁定
     */
    private Boolean outputTypeLocked;

    /**
     * 是否只有提示词可以修改（内置字段）：其余项锁定，列表里的启用开关也不可操作
     */
    private Boolean promptOnly;
}
