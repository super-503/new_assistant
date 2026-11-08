package com.zengame.ai.business.news.setting.domain.vo;

import lombok.Value;

/** 模型设置页使用的配置和密钥状态。 */
@Value
public class NewsAiModelSettingVO {

    boolean configured;
    String modelUrl;
    String model;
    boolean apiKeyConfigured;
}
