package com.zengame.ai.business.news.setting.domain.vo;

import lombok.Value;

/** 通知配置页可选择的飞书授权人。 */
@Value
public class NewsFeishuAuthorizedUserVO {
    String openId;
    String name;
    String avatarUrl;
}
