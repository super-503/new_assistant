package com.zengame.ai.repository.ai.po;

import com.zengame.ai.business.news.domain.enums.NewsCredentialStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资讯渠道（news_channel），与渠道插件一一对应。凭证只属于渠道，同一渠道下的订阅源共用。
 */
@Data
public class NewsChannelPO {

    private Long id;

    /**
     * 渠道标识，与插件 {@code ChannelDescriptor#getCode()} 一致
     */
    private String code;

    /**
     * 渠道名称
     */
    private String name;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 渠道凭证，插件自定义的不透明 JSON；无凭证或未登录时为空
     */
    private String credentialJson;

    /**
     * 凭证状态
     */
    private NewsCredentialStatus credentialStatus;

    /**
     * 凭证对应的登录账号
     */
    private String credentialAccount;

    /**
     * 凭证最近一次保存时间
     */
    private LocalDateTime credentialUpdateTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
