package com.zengame.ai.business.news.channel;

import com.zengame.ai.business.news.channel.bo.ChannelCredential;

import java.util.Optional;

/**
 * 能自动续期凭证的渠道插件。每次拉取该渠道前，核心流程先调用续期。
 */
public interface CredentialRefreshable {

    /**
     * 检查凭证是否需要刷新，需要时按平台流程刷新。
     *
     * @param credential 当前渠道凭证
     * @return 刷新后的新凭证，核心流程写回渠道表；无需刷新时为空
     * @throws ChannelLoginExpiredException 凭证已失效或刷新失败
     */
    Optional<ChannelCredential> refreshIfNeeded(ChannelCredential credential);
}
