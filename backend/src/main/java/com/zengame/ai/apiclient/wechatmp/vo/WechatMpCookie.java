package com.zengame.ai.apiclient.wechatmp.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信读书的会话 Cookie。
 */
@Data
@lombok.ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class WechatMpCookie {

    /**
     * Cookie 名称
     */
    private String name;

    /**
     * Cookie 值
     */
    private String value;
}
