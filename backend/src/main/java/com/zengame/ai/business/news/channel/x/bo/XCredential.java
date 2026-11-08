package com.zengame.ai.business.news.channel.x.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * X 渠道凭证，存于 news_channel.credential_json。只存 auth_token，ct0 在运行时由 auth_token 换取。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class XCredential {

    /**
     * 网页端登录 cookie auth_token
     */
    private String authToken;

    /**
     * 不输出 auth_token，避免日志泄露登录态。
     */
    @Override
    public String toString() {
        return "XCredential[authToken=***]";
    }
}
