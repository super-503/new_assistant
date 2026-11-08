package com.zengame.ai.apiclient.bilibili.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * B站的一个 cookie，取自响应的 Set-Cookie，请求时拼进 Cookie 请求头。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BilibiliCookie {

    /**
     * cookie 名，如 SESSDATA、bili_jct、DedeUserID、buvid3
     */
    private String name;

    /**
     * cookie 值
     */
    private String value;
}
