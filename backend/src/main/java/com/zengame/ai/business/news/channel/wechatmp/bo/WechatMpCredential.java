package com.zengame.ai.business.news.channel.wechatmp.bo;

import com.zengame.ai.apiclient.wechatmp.vo.WechatMpCookie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 公众号渠道凭证：扫码登录微信读书后的会话 Cookie。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WechatMpCredential {

    /**
     * 微信读书会话 Cookie
     */
    private List<WechatMpCookie> cookies;

}
