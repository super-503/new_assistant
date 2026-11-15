package com.zengame.ai.business.news.setting.domain.request;

import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuCardSettings;
import com.zengame.ai.business.news.setting.domain.bo.NewsDigestContentSettings;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.util.List;

/** 管理员编辑的完整飞书渠道配置。 */
@Getter
@Setter
public class NewsFeishuChannelSaveRequest {
    @NotNull
    @PositiveOrZero
    private Long revision;

    @NotNull
    @Size(max = 20)
    private List<@NotNull @Valid App> apps;

    @NotNull
    @Size(max = 100)
    private List<@NotNull @Valid Recipient> recipients;

    @NotNull
    @Valid
    private NewsFeishuCardSettings card;

    @Valid
    private NewsDigestContentSettings content;

    private boolean enabled;

    @Getter
    @Setter
    public static class App extends NewsFeishuAppRequest {
        @Size(max = 255)
        private String name;

        @Size(max = 2048)
        @Pattern(regexp = "https://[^\\s]+")
        private String avatarUrl;
    }

    @Getter
    @Setter
    public static class Recipient {
        @NotNull
        @Size(max = 128)
        @Pattern(regexp = "cli_[a-zA-Z0-9_-]+")
        private String appId;

        @NotNull
        @Size(max = 128)
        @Pattern(regexp = "ou_[a-zA-Z0-9_-]+")
        private String openId;

        @Size(max = 255)
        private String name;

        @Size(max = 2048)
        @Pattern(regexp = "https://[^\\s]+")
        private String avatarUrl;
    }
}
