package com.zengame.ai.business.news.channel;

import com.zengame.ai.business.news.channel.bo.ChannelCredential;
import com.zengame.ai.business.news.channel.bo.CredentialVerifyResult;

/**
 * 能立即检查凭证是否可用的插件，渠道设置里的「验证连接」和 Token 保存前的校验都走它。
 * 由 {@link QrLoginCapable} 和 {@link TokenCredential} 继承，需要凭证的渠道都具备。
 */
public interface CredentialVerifiable {

    /**
     * 用凭证查询当前登录账号，判断凭证是否可用。
     *
     * @param credential 待校验的渠道凭证
     * @return 可用时带登录账号，不可用时带失败原因；网络等非凭证问题直接抛出异常
     */
    CredentialVerifyResult verify(ChannelCredential credential);
}
