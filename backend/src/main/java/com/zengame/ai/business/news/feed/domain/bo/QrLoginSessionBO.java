package com.zengame.ai.business.news.feed.domain.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 扫码登录会话，以 JSON 存于 Redis {@code news:qr:{sessionId}}，有效期与平台二维码一致。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrLoginSessionBO {

    /**
     * 发起扫码的渠道标识，轮询时校验会话属于该渠道
     */
    private String channelCode;

    /**
     * 插件自定义的扫码会话状态，轮询时原样传给插件
     */
    private String sessionState;

    /**
     * 二维码过期时刻（epoch 毫秒），会话写回 Redis 时据此计算剩余有效期
     */
    private long expireAtMillis;
}
