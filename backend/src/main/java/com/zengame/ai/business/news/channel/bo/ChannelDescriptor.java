package com.zengame.ai.business.news.channel.bo;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsCredentialMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 渠道插件的自我描述：核心流程和前端按它区分渠道能力，代码里不出现具体渠道标识。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDescriptor {

    /**
     * 渠道标识，与 news_channel.code 一一对应，如 wechat_mp、bilibili、x、aibase
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
     * 新增订阅源时默认选中的内容类型
     */
    private NewsContentType defaultContentType;

    /**
     * 凭证方式，决定渠道设置里凭证区的形态和插件必须实现的能力接口
     */
    private NewsCredentialMode credentialMode;

    /**
     * 是否支持按关键词搜索来源；不支持时添加订阅源直接展示唯一的来源
     */
    private boolean searchable;

    /**
     * 搜索框的提示文字，说明该渠道按什么搜索；不支持搜索的渠道为 null
     */
    private String searchHint;

    /**
     * 正文里允许保留的 iframe 播放器域名，按 src 的主机名完全匹配；没有时为空列表
     */
    private List<String> embedDomains;
}
