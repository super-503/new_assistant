package com.zengame.ai.business.news.setting.domain.bo;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/** 飞书渠道保存的应用凭证、应用内接收人和卡片样式。 */
@Data
public class NewsFeishuChannelConfig {
    private List<App> apps = new ArrayList<>();
    private List<Recipient> recipients = new ArrayList<>();
    private NewsFeishuCardSettings card = new NewsFeishuCardSettings();
    private NewsDigestContentSettings content;
    private boolean enabled;

    @Data
    public static class App {
        private String appId;
        @ToString.Exclude
        private String appSecret;
        private String name;
        private String avatarUrl;
    }

    @Data
    public static class Recipient {
        private String appId;
        private String openId;
        private String name;
        private String avatarUrl;
    }
}
