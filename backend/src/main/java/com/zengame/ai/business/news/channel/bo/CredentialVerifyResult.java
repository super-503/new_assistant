package com.zengame.ai.business.news.channel.bo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 凭证校验的结果：凭证可用时带登录账号，不可用时带给运营看的失败原因。
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CredentialVerifyResult {

    /**
     * 凭证是否可用
     */
    private final boolean valid;

    /**
     * 凭证可用时的登录账号；不可用时为空
     */
    private final String account;

    /**
     * 凭证不可用时的失败原因，如「auth_token 已失效」；可用时为空
     */
    private final String message;

    /**
     * @param account 登录账号
     * @return 凭证可用的结果
     */
    public static CredentialVerifyResult valid(String account) {
        return new CredentialVerifyResult(true, account, null);
    }

    /**
     * @param message 失败原因
     * @return 凭证不可用的结果
     */
    public static CredentialVerifyResult invalid(String message) {
        return new CredentialVerifyResult(false, null, message);
    }
}
