package com.zengame.ai.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 资讯采集、模型服务、通知和渠道配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "news")
public class NewsProperties {

    private String adminUrl;
    private String model;
    private String modelUrl;
    private String modelApiKey;
    private String feishuWebhook;
    private String digestFeishuWebhook;
    private String feishuAuthorizationRedirectUri;
    private String timeZone = "Asia/Shanghai";
    private boolean jobFamilyEnabled;
    private boolean syncJobFamilies;
    private XChannel x;

    @Data
    public static class XChannel {

        private String proxyHost;
        private int proxyPort;
    }
}
