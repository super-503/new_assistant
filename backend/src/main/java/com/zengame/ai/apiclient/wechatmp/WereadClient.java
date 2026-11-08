package com.zengame.ai.apiclient.wechatmp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.zengame.ai.apiclient.wechatmp.vo.WechatMpCookie;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import lombok.Value;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 微信读书网页版扫码登录与公众号内容接口。
 */
@Component
public class WereadClient {

    private static final String ORIGIN = "https://weread.qq.com";
    private static final HttpUrl BASE_URL = HttpUrl.get(ORIGIN);
    private static final String VID = "wr_vid";
    private static final String SESSION_KEY = "wr_skey";
    private static final Set<String> LOGIN_ERRORS = Set.of("-2010", "-2012", "-2013", "-2041");
    private static final Set<String> WAITING_CODES = Set.of("0", "LOGIN_TIMEOUT");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false)
            .build();

    private final OkHttpClient pollingClient = httpClient.newBuilder()
            .readTimeout(70, TimeUnit.SECONDS)
            .build();

    /**
     * 申请扫码会话。
     *
     * @return 用于构造 /web/confirm?uid=… 二维码载荷的 uid
     */
    public String startLogin() {
        JsonNode data = getJson(url("/api/auth/getLoginUid").build(), List.of());
        return requiredText(data.has("uid") ? data : data.path("data"), "uid");
    }

    /**
     * 构造由微信扫码打开的登录确认地址。
     *
     * @param uid 扫码会话标识
     * @return 二维码载荷
     */
    public String qrUrl(String uid) {
        return url("/web/confirm").addQueryParameter("uid", uid).build().toString();
    }

    /**
     * 查询扫码结果。扫码会话的整体有效期由调用方管理。
     *
     * @param uid 扫码会话标识
     * @return 当前扫码状态，确认后带登录 Cookie
     * @throws BusinessException 上游拒绝登录或要求验证码
     */
    public LoginPoll pollLogin(String uid) {
        HttpUrl url = url("/api/auth/getLoginInfo")
                .addQueryParameter("uid", uid)
                .addQueryParameter("otp", "")
                .build();
        return execute(pollingClient, request(url, List.of()).build(), response -> {
            JsonNode data = readJson(response);
            JsonNode inner = data.path("data");
            if (data.path("succeed").asBoolean() || inner.path("succeed").asBoolean()) {
                List<WechatMpCookie> cookies = parseCookies(response);
                requireCookie(cookies, VID);
                requireCookie(cookies, SESSION_KEY);
                return new LoginPoll(LoginState.CONFIRMED, cookies);
            }
            String code = data.path("logicCode").asText(inner.path("logicCode").asText());
            if (WAITING_CODES.contains(code)) {
                return new LoginPoll(LoginState.WAITING, List.of());
            }
            if ("1".equals(code)) {
                return new LoginPoll(LoginState.SCANNED, List.of());
            }
            if ("NEED_OTP".equals(code)) {
                throw new BusinessException("微信读书登录需要验证码");
            }
            throw new BusinessException("微信读书返回无法识别的扫码状态");
        });
    }

    /**
     * 验证微信读书登录凭证并查询当前账号。
     *
     * @param cookies 登录 Cookie
     * @return 账号显示名称
     * @throws SessionExpiredException 凭证失效或响应账号与凭证不匹配
     */
    public String account(List<WechatMpCookie> cookies) {
        String vid = requireCookie(cookies, VID);
        verifySession(cookies);
        JsonNode data = getJson(url("/web/user").addQueryParameter("userVid", vid).build(), cookies);
        if (!vid.equals(data.path("userVid").asText())) {
            throw new SessionExpiredException();
        }
        return requiredText(data, "name");
    }

    /**
     * 通过需要登录的书架接口检查会话是否可用。
     *
     * @param cookies 登录 Cookie
     * @throws SessionExpiredException 会话已失效
     */
    public void verifySession(List<WechatMpCookie> cookies) {
        requireCookie(cookies, VID);
        requireCookie(cookies, SESSION_KEY);
        JsonNode shelf = getJson(url("/web/shelf/sync").addQueryParameter("userVid", "")
                .addQueryParameter("synckey", "0").addQueryParameter("lectureSynckey", "0").build(), cookies);
        if (!shelf.path("books").isArray()) {
            throw new BusinessException("微信读书书架响应缺少书籍列表");
        }
    }

    /**
     * 查询公众号资料。
     *
     * @param cookies 登录 Cookie
     * @param bookId 公众号的微信读书编号
     * @return 公众号名称、头像和简介
     */
    public BookInfo bookInfo(List<WechatMpCookie> cookies, String bookId) {
        JsonNode data = getJson(url("/web/book/info").addQueryParameter("bookId", bookId).build(), cookies);
        return new BookInfo(requiredText(data, "title"), data.path("cover").asText(""), data.path("intro").asText(""));
    }

    /**
     * 查询公众号最新一篇文章。
     *
     * @param cookies 登录 Cookie
     * @param bookId 公众号的微信读书编号
     * @return 文章标题、封面、正文标识及公众号名称和头像
     */
    public Cover cover(List<WechatMpCookie> cookies, String bookId) {
        JsonNode data = getJson(url("/api/mp/cover").addQueryParameter("bookId", bookId).build(), cookies);
        return new Cover(requiredText(data, "title"), data.path("pic").asText(""), requiredText(data, "reviewId"),
                data.path("name").asText(""), data.path("avatar").asText(""));
    }

    /**
     * 获取微信读书承载的公众号文章页面。
     *
     * @param cookies 登录 Cookie
     * @param reviewId 文章正文标识
     * @return 包含公众号正文及页面元数据的完整文档
     */
    public Document content(List<WechatMpCookie> cookies, String reviewId) {
        HttpUrl url = url("/web/mp/content").addQueryParameter("reviewId", reviewId).build();
        return execute(httpClient, request(url, cookies).build(), response -> {
            String body = readBody(response);
            if (StringUtils.stripStart(body, null).startsWith("{")) {
                parseJson(body);
                throw new BusinessException("微信读书未返回文章页面");
            }
            Document document = Jsoup.parse(body, url.toString());
            if (document.getElementById("js_content") == null) {
                throw new BusinessException("微信读书文章页面缺少正文");
            }
            return document;
        });
    }

    private static HttpUrl.Builder url(String path) {
        return BASE_URL.newBuilder().encodedPath(path);
    }

    private static Request.Builder request(HttpUrl url, List<WechatMpCookie> cookies) {
        Request.Builder request = new Request.Builder().url(url)
                .header("Origin", ORIGIN)
                .header("Referer", ORIGIN + "/")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36");
        if (!cookies.isEmpty()) {
            request.header("Cookie", cookies.stream().map(WereadClient::cookiePair).collect(Collectors.joining("; ")));
        }
        return request;
    }

    private static String cookiePair(WechatMpCookie cookie) {
        String name = cookie.getName();
        String value = cookie.getValue();
        if (StringUtils.isEmpty(name) || value == null || !name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
                || !value.matches("[\\x21\\x23-\\x2B\\x2D-\\x3A\\x3C-\\x5B\\x5D-\\x7E]*")) {
            throw new BusinessException("微信读书 Cookie 格式无效");
        }
        return name + "=" + value;
    }

    private static List<WechatMpCookie> parseCookies(Response response) {
        Map<String, WechatMpCookie> merged = new LinkedHashMap<>();
        for (Cookie cookie : Cookie.parseAll(response.request().url(), response.headers())) {
            if (!cookie.matches(BASE_URL)) {
                continue;
            }
            if (cookie.expiresAt() <= System.currentTimeMillis() || cookie.value().isEmpty()) {
                merged.remove(cookie.name());
            } else {
                merged.put(cookie.name(), new WechatMpCookie(cookie.name(), cookie.value()));
            }
        }
        return List.copyOf(merged.values());
    }

    private static String requireCookie(List<WechatMpCookie> cookies, String name) {
        return cookies.stream()
                .filter(cookie -> name.equals(cookie.getName()))
                .map(WechatMpCookie::getValue)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElseThrow(SessionExpiredException::new);
    }

    private JsonNode getJson(HttpUrl url, List<WechatMpCookie> cookies) {
        return execute(httpClient, request(url, cookies).build(), WereadClient::readJson);
    }

    private static JsonNode readJson(Response response) {
        return parseJson(readBody(response));
    }

    private static JsonNode parseJson(String body) {
        JsonNode data;
        try {
            data = JacksonUtil.getObjectMapper().readTree(body);
        } catch (JsonProcessingException e) {
            throw new BusinessException("微信读书返回无效的 JSON");
        }
        if (data == null || !data.isObject()) {
            throw new BusinessException("微信读书返回无效的 JSON 对象");
        }
        checkError(data);
        checkError(data.path("data"));
        return data;
    }

    private static void checkError(JsonNode data) {
        String code = data.path("errCode").asText(data.path("errcode").asText("0"));
        if (LOGIN_ERRORS.contains(code)) {
            throw new SessionExpiredException("错误码 " + code);
        }
        if (!"0".equals(code)) {
            throw new BusinessException("微信读书接口返回业务错误");
        }
    }

    private static String requiredText(JsonNode data, String field) {
        String value = data.path(field).asText("");
        if (StringUtils.isBlank(value)) {
            throw new BusinessException("微信读书响应缺少字段 " + field);
        }
        return value;
    }

    private static String readBody(Response response) {
        ResponseBody body = response.body();
        if (body == null) {
            throw new BusinessException("微信读书响应缺少正文");
        }
        try {
            return body.string();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static <T> T execute(OkHttpClient client, Request request, Function<Response, T> read) {
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 401) {
                throw new SessionExpiredException();
            }
            if (!response.isSuccessful()) {
                throw new BusinessException("微信读书请求失败 " + request.url().encodedPath() + " HTTP " + response.code());
            }
            return read.apply(response);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 公众号资料。 */
    @Value
    public static class BookInfo {
        String title;
        String cover;
        String intro;
    }

    /** 公众号最新文章及发布账号。 */
    @Value
    public static class Cover {
        String title;
        String pic;
        String reviewId;
        String name;
        String avatar;
    }

    public enum LoginState {
        WAITING, SCANNED, CONFIRMED
    }

    /** 扫码状态及已授权的登录 Cookie。 */
    @Value
    public static class LoginPoll {
        LoginState state;
        List<WechatMpCookie> cookies;
    }

    /** 微信读书登录凭证失效，需要重新扫码授权。 */
    public static class SessionExpiredException extends BusinessException {
        public SessionExpiredException() {
            super("微信读书登录已失效，请重新扫码授权");
        }

        public SessionExpiredException(String reason) {
            super("微信读书登录已失效，请重新扫码授权（" + reason + "）");
        }
    }
}
