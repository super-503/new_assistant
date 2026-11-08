package com.zengame.ai.business.news.channel.bilibili.bo;

import com.zengame.ai.apiclient.bilibili.vo.BilibiliCookie;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * B站扫码会话状态，以 JSON 作为 sessionState 由核心层存入 Redis。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BilibiliQrSession {

    /**
     * 扫码登录密钥
     */
    private String qrcodeKey;

    /**
     * 发起扫码时取得的设备 cookie（buvid3、buvid4），登录成功后并入凭证
     */
    private List<BilibiliCookie> deviceCookies;
}
