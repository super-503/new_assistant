package com.zengame.ai.repository.ai.po;

import lombok.Data;
import lombok.ToString;

/** 飞书渠道的配置版本和完整配置。 */
@Data
public class NewsFeishuChannelConfigPO {
    private long revision;
    @ToString.Exclude
    private String configJson;
}
