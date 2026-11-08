package com.zengame.ai.business.news.channel;

import com.zengame.ai.business.news.channel.bo.ChannelCredential;

/**
 * 以粘贴 Token 维护凭证的渠道插件（X），凭证方式为 TOKEN。
 *
 * <p>保存流程：{@link #createCredential(String)} 组装凭证 → {@link #verify} 校验，可用才写入渠道表。
 */
public interface TokenCredential extends CredentialVerifiable {

    /**
     * 把运营粘贴的 Token 组装成插件自己的凭证。
     *
     * @param token 运营粘贴的 Token，如 X 的 auth_token
     * @return 渠道凭证
     */
    ChannelCredential createCredential(String token);
}
