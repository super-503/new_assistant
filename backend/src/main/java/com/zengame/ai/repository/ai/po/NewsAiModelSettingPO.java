package com.zengame.ai.repository.ai.po;

import lombok.Getter;
import lombok.Setter;

/** 资讯 AI 模型配置。 */
@Getter
@Setter
public class NewsAiModelSettingPO {

    private String modelUrl;
    private String model;
    private String apiKey;
}
