package com.zengame.ai.business.news.setting.domain.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 新增或编辑 AI 字段。编辑时按字段种类限制可修改的项，见 {@code NewsAiFieldSettingService}。
 */
@Data
public class NewsAiFieldSaveRequest {

    @NotBlank(message = "字段名称不能为空")
    @Size(max = 32, message = "字段名称不能超过 32 个字符")
    private String name;

    /**
     * 字段标识，结构化结果按它存 JSON
     */
    @NotBlank(message = "字段标识不能为空")
    @Size(max = 64, message = "字段标识不能超过 64 个字符")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "字段标识只能包含小写字母、数字和下划线，并以小写字母开头")
    private String fieldKey;

    /**
     * 来源内容：BODY / TITLE / TITLE_BODY
     */
    @NotBlank(message = "来源字段不能为空")
    private String source;

    /**
     * 输出类型：TEXT / NUMBER / SINGLE / MULTI
     */
    @NotBlank(message = "输出类型不能为空")
    private String outputType;

    /**
     * 文本是否多行；只有文本可以开启
     */
    @NotNull(message = "多行不能为空")
    private Boolean multiline;

    /**
     * 单选、多选的选项；其他输出类型传空数组
     */
    @NotNull(message = "选项不能为空")
    private List<String> options;

    /**
     * 是否显示为标签；只有单选、多选可以开启
     */
    @NotNull(message = "显示为标签不能为空")
    private Boolean showAsTag;

    @NotBlank(message = "提示词不能为空")
    @Size(max = 2000, message = "提示词不能超过 2000 个字符")
    private String prompt;

    @NotNull(message = "必填不能为空")
    private Boolean required;

    /**
     * 是否允许人工修改
     */
    @NotNull(message = "允许人工修改不能为空")
    private Boolean editable;

    @NotNull(message = "启用不能为空")
    private Boolean enabled;
}
