package com.zengame.ai.business.news.setting.domain.vo;

import lombok.Value;

/** 飞书授权页和本次授权的查询标识。 */
@Value
public class NewsFeishuAuthorizationStartVO {
    String state;
    String authorizationUrl;
    long expiresIn;
}
