package com.zengame.ai.business.news.channel.bilibili.bo;

import com.zengame.ai.apiclient.bilibili.vo.BilibiliCookie;
import com.zengame.ai.business.news.channel.ChannelLoginExpiredException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * B站渠道凭证：网页端登录后的完整 cookie 和 refresh_token，以 JSON 存于 news_channel.credential_json。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BilibiliCredential {

    private static final String CSRF_COOKIE = "bili_jct";

    /**
     * 登录 cookie：SESSDATA、bili_jct、DedeUserID、DedeUserID__ckMd5、sid，以及设备标识 buvid3、buvid4
     */
    private List<BilibiliCookie> cookies;

    /**
     * 刷新 cookie 用的 refresh_token
     */
    private String refreshToken;

    /**
     * @return Cookie 请求头
     */
    public String cookieHeader() {
        return cookies.stream()
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; "));
    }

    /**
     * @return cookie 中的 bili_jct，即写接口的 csrf 参数
     * @throws ChannelLoginExpiredException 凭证里没有 bili_jct
     */
    public String csrf() {
        return cookies.stream()
                .filter(cookie -> CSRF_COOKIE.equals(cookie.getName()))
                .map(BilibiliCookie::getValue)
                .findFirst()
                .orElseThrow(() -> new ChannelLoginExpiredException("B站凭证缺少 bili_jct"));
    }

    /**
     * 合并两组 cookie，同名的以后者为准。
     *
     * @param base      原有 cookie
     * @param overrides 新下发的 cookie
     * @return 合并后的 cookie
     */
    public static List<BilibiliCookie> mergeCookies(List<BilibiliCookie> base, List<BilibiliCookie> overrides) {
        Map<String, BilibiliCookie> merged = new LinkedHashMap<>();
        Stream.concat(base.stream(), overrides.stream()).forEach(cookie -> merged.put(cookie.getName(), cookie));
        return new ArrayList<>(merged.values());
    }
}
