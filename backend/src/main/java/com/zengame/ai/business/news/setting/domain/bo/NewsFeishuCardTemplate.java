package com.zengame.ai.business.news.setting.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Map;

/** 飞书卡片搭建工具中已发布的模板及其变量配置。 */
@Data
public class NewsFeishuCardTemplate {
    @NotBlank(message = "请填写飞书卡片模板 ID")
    @Pattern(regexp = "[a-zA-Z0-9_-]{1,128}")
    private String id;

    @Pattern(regexp = "(?:[0-9]+\\.[0-9]+\\.[0-9]+)?")
    @Size(max = 32)
    private String version;

    @NotBlank
    @Size(max = 8000)
    private String variablesJson = "{\"content\":\"${content}\"}";

    @Size(max = 100)
    private Map<@Size(max = 128) String,
            @Pattern(regexp = "text|number|boolean|url|object|array|json") String> variableTypes;
}
