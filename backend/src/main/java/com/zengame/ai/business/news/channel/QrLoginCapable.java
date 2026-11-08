package com.zengame.ai.business.news.channel;

import com.zengame.ai.business.news.channel.bo.QrLoginPoll;
import com.zengame.ai.business.news.channel.bo.QrLoginStart;

/**
 * 支持扫码登录的渠道插件（公众号、B站），凭证方式为 QR。
 *
 * <p>扫码会话状态由插件自定义、核心层存于 Redis，插件本身不保存会话。
 */
public interface QrLoginCapable extends CredentialVerifiable {

    /**
     * 向平台发起扫码登录，生成二维码。
     *
     * @return 二维码图片、扫码会话状态和有效期
     */
    QrLoginStart startQrLogin();

    /**
     * 查询一次扫码结果；确认登录时完成平台的登录流程并组装凭证。
     *
     * @param sessionState 上次发起或轮询返回的扫码会话状态
     * @return 扫码状态；CONFIRMED 时带新凭证和登录账号
     */
    QrLoginPoll pollQrLogin(String sessionState);
}
