package com.zengame.ai.business.news.channel.bo;

import com.zengame.ai.business.news.domain.enums.NewsQrLoginStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 扫码登录一次轮询的结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrLoginPoll {

    /**
     * 扫码状态
     */
    private NewsQrLoginStatus status;

    /**
     * WAITING、SCANNED 时为更新后的扫码会话状态，核心层写回 Redis 供下次轮询；其余状态为空
     */
    private String sessionState;

    /**
     * CONFIRMED 时为新凭证，核心层写入渠道表并把凭证状态置为有效；其余状态为空
     */
    private ChannelCredential credential;

    /**
     * CONFIRMED 时为登录账号，写入 news_channel.credential_account；其余状态为空
     */
    private String account;
}
