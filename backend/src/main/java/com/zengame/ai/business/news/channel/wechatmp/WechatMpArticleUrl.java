package com.zengame.ai.business.news.channel.wechatmp;

import com.zengame.ai.infrastructure.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 公众号文章链接：规范化为去重用的原文链接，以及还原为可请求的地址。
 */
public final class WechatMpArticleUrl {

    private static final String HOST = "mp.weixin.qq.com";

    /**
     * 永久长链接中标识一篇文章的参数，其余参数（chksm、scene 等）与文章无关
     */
    private static final List<String> ESSENTIAL_PARAMS = List.of("__biz", "mid", "idx", "sn");

    private WechatMpArticleUrl() {
    }

    /**
     * 把文章链接规范化为原文链接：长链接 {@code /s?...} 只保留 __biz、mid、idx、sn；短链接 {@code /s/xxx} 保留路径；
     * 统一为 https，去掉锚点。
     *
     * @param link 文章链接，可能带 HTML 转义的 {@code &amp;}
     * @return 规范化后的原文链接
     * @throws BusinessException 不是公众号文章链接，或长链接缺少必要参数
     */
    public static String canonicalize(String link) {
        UriComponents uri = UriComponentsBuilder.fromUriString(toRequestUrl(link)).build();
        String path = StringUtils.defaultString(uri.getPath());
        if (path.startsWith("/s/")) {
            return "https://" + HOST + path;
        }
        if (!"/s".equals(path)) {
            throw new BusinessException("无法识别的公众号文章链接：" + link);
        }
        MultiValueMap<String, String> params = uri.getQueryParams();
        if (ESSENTIAL_PARAMS.stream().anyMatch(name -> StringUtils.isBlank(params.getFirst(name)))) {
            throw new BusinessException("公众号文章长链接缺少 __biz、mid、idx、sn 参数：" + link);
        }
        return "https://" + HOST + "/s?" + ESSENTIAL_PARAMS.stream()
                .map(name -> name + "=" + params.getFirst(name))
                .collect(Collectors.joining("&"));
    }

    /**
     * 把文章列表返回的链接还原为可请求的 https 地址，保留全部查询参数，去掉锚点。
     *
     * @param link 文章链接，可能带 HTML 转义的 {@code &amp;}
     * @return 可请求的文章地址
     * @throws BusinessException 主机不是 mp.weixin.qq.com
     */
    public static String toRequestUrl(String link) {
        UriComponents uri = UriComponentsBuilder.fromUriString(StringUtils.replace(link, "&amp;", "&")).build();
        if (!HOST.equals(uri.getHost())) {
            throw new BusinessException("不是公众号文章链接：" + link);
        }
        return UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(HOST)
                .path(uri.getPath())
                .query(uri.getQuery())
                .build()
                .toUriString();
    }
}
