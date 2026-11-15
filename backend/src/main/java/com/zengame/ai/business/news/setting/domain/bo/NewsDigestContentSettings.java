package com.zengame.ai.business.news.setting.domain.bo;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.setting.domain.enums.NewsDigestDeliveryChannel;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/** 日报选取的已审核资讯范围和每条资讯显示的字段。 */
@Data
public class NewsDigestContentSettings {
    @NotNull
    @Size(max = 16)
    private List<@NotNull NewsDigestDeliveryChannel> deliveryChannels = List.of(NewsDigestDeliveryChannel.FEISHU);

    @NotNull
    @Size(max = 50)
    private List<@NotNull @Pattern(regexp = "[a-zA-Z0-9_-]{1,64}") String> channelCodes = List.of();

    @NotNull
    @Size(max = 2)
    private List<@NotNull NewsContentType> contentTypes = List.of();

    @Min(1)
    @Max(365)
    private Integer reviewDays;

    @NotNull
    @Size(max = 32)
    private List<@NotNull @Pattern(regexp = "author|publishedAt|url|ai:[a-zA-Z0-9_]{1,64}") String> fields =
            List.of("author", "url");

    private boolean useAiTitle;
    private boolean compact;
}
