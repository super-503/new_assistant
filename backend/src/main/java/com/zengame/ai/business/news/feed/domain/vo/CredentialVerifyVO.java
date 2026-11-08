package com.zengame.ai.business.news.feed.domain.vo;

import lombok.Data;

/**
 * 「验证连接」的结果。
 */
@Data
public class CredentialVerifyVO {

    /**
     * 凭证是否可用，可用时显示「连接正常」
     */
    private boolean valid;

    /**
     * 凭证可用时的登录账号；不可用时为空
     */
    private String account;

    /**
     * 凭证不可用时的失败原因；可用时为空
     */
    private String message;
}
