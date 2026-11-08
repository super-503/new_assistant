package com.zengame.ai.business.news.setting.domain.bo;

import lombok.ToString;
import lombok.Value;

/** 资讯加工使用的模型配置。 */
@Value
@ToString(onlyExplicitlyIncluded = true)
public class NewsAiModelConfig {

    String modelUrl;
    String model;
    String apiKey;
}
