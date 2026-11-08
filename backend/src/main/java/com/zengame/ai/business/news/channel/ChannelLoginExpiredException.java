package com.zengame.ai.business.news.channel;

import com.zengame.ai.infrastructure.exception.BusinessException;

/**
 * 渠道登录失效：插件确认平台凭证不可用时抛出。
 *
 * <p>拉取时核心流程据此把渠道凭证置为已失效、发飞书通知并中止该渠道；搜索、预览等接口调用时原因直接提示给运营。
 */
public class ChannelLoginExpiredException extends BusinessException {

    /**
     * @param reason 失效原因，写进飞书通知和接口提示，如「自动续期未成功」
     */
    public ChannelLoginExpiredException(String reason) {
        super(reason);
    }

    /**
     * @param reason 失效原因，写进飞书通知和接口提示
     * @param cause  平台返回的原始异常
     */
    public ChannelLoginExpiredException(String reason, Throwable cause) {
        super(reason);
        initCause(cause);
    }
}
