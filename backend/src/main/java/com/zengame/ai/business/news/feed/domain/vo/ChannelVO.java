package com.zengame.ai.business.news.feed.domain.vo;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsCredentialMode;
import com.zengame.ai.business.news.domain.enums.NewsCredentialStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 渠道配置页左侧的一个渠道，也是渠道头部和渠道设置弹窗的数据来源。
 */
@Data
public class ChannelVO {

    /**
     * 渠道标识，只作为接口参数回传，前端不按它分支
     */
    private String code;

    /**
     * 渠道名称
     */
    private String name;

    /**
     * 原文来源域名，来源卡片上显示为「原文来源 域名」
     */
    private String domain;

    /**
     * 凭证方式，决定渠道设置里凭证区的形态：NONE 不显示 / QR 扫码 / TOKEN 输入 Token
     */
    private NewsCredentialMode credentialMode;

    /**
     * 是否支持按关键词搜索来源；不支持时添加订阅源不显示搜索框，直接展示唯一的来源
     */
    private boolean searchable;

    /**
     * 搜索框的提示文字；不支持搜索的渠道为 null
     */
    private String searchHint;

    /**
     * 新增订阅源时默认选中的内容类型
     */
    private NewsContentType defaultContentType;

    /**
     * 是否启用渠道
     */
    private Boolean enabled;

    /**
     * 凭证状态：NONE 无需凭证（不显示）/ VALID 有效 / INVALID 已失效
     */
    private NewsCredentialStatus credentialStatus;

    /**
     * 凭证状态的中文名
     */
    private String credentialStatusDesc;

    /**
     * 登录账号；未登录时为空
     */
    private String credentialAccount;

    /**
     * 凭证最近一次保存时间；未登录时为空
     */
    private LocalDateTime credentialUpdateTime;

    /**
     * 订阅源数量
     */
    private int feedCount;

    /**
     * 是否需要处理：凭证已失效，或有订阅源拉取失败；为 true 时名称旁显示红色状态点
     */
    private boolean needsAttention;
}
