package com.zengame.ai.apiclient.bilibili.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 一次扫码轮询的结果：扫码状态码，登录成功时带登录 cookie 和 refresh_token。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BilibiliQrPollResult {

    /**
     * 扫码状态码：0 登录成功、86101 未扫码、86090 已扫码未确认、86038 二维码已失效
     */
    private int statusCode;

    /**
     * 状态说明
     */
    private String statusMessage;

    /**
     * 登录成功时的 refresh_token；其余状态为空
     */
    private String refreshToken;

    /**
     * 登录成功时响应 Set-Cookie 下发的登录 cookie；其余状态为空列表
     */
    private List<BilibiliCookie> cookies;
}
