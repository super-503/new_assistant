package com.zengame.ai.apiclient.wechatmp;

import com.zengame.ai.infrastructure.exception.BusinessException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 请求公开的公众号文章页和媒体资源。
 */
@Component
public class WechatMpClient {

    private static final String REFERER = "https://mp.weixin.qq.com/";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * @param url 公开的公众号文章链接
     * @return 文章页面
     * @throws BusinessException 页面请求失败或被平台拦截
     */
    public Document fetchArticlePage(String url) {
        try (Response response = execute(url)) {
            HttpUrl finalUrl = response.request().url();
            Document document = Jsoup.parse(body(response).string(), finalUrl.toString());
            if (finalUrl.encodedPath().contains("captcha")
                    || document.select(".weui-msg__title").text().contains("环境异常")) {
                throw new BusinessException("公众号文章页被平台拦截（环境异常 / 验证码）");
            }
            return document;
        } catch (IOException e) {
            throw new UncheckedIOException("公众号文章请求失败", e);
        }
    }

    private Response execute(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", REFERER)
                .build();
        Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            int code = response.code();
            response.close();
            throw new BusinessException("公众号请求失败：HTTP " + code);
        }
        return response;
    }

    private ResponseBody body(Response response) {
        return Objects.requireNonNull(response.body(), "公众号响应缺少内容");
    }
}
