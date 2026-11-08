package com.zengame.ai.business.news.setting.domain.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 资讯 AI 模型表单。密钥为 null 时沿用同一地址的密钥，空串清除密钥。 */
@Getter
@Setter
public class NewsAiModelSaveRequest {

    @NotBlank
    @Size(max = 2048)
    private String modelUrl;

    @NotBlank
    @Size(max = 255)
    private String model;

    @Size(max = 2048)
    private String apiKey;
}
