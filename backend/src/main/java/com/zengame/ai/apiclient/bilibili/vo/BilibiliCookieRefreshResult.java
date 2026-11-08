package com.zengame.ai.apiclient.bilibili.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 刷新 cookie 的结果：新的登录 cookie 和新的 refresh_token。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BilibiliCookieRefreshResult {

    /**
     * 新的 refresh_token
     */
    private String refreshToken;

    /**
     * 响应 Set-Cookie 下发的新登录 cookie
     */
    private List<BilibiliCookie> cookies;
}
