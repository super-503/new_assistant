package com.zengame.ai.apiclient.x;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.zengame.ai.apiclient.x.vo.XApiDocument;
import com.zengame.ai.apiclient.x.vo.XGraphqlEndpoint;
import com.zengame.ai.business.news.channel.ChannelLoginExpiredException;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import lombok.RequiredArgsConstructor;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * X 网页端接口客户端：所有请求（x.com、pbs.twimg.com、video.twimg.com）都经 {@code news.x} 配置的 HTTP 代理。
 *
 * <p>登录态只需 auth_token：首次使用时带 auth_token 访问 x.com，从 Set-Cookie 取得网页端 CSRF 令牌 ct0，
 * 按 auth_token 缓存在内存；接口请求同时带上公开 Bearer、{@code x-csrf-token} 和 auth_token / ct0 两个 cookie。
 * 公开 Bearer、网页端 User-Agent 和 GraphQL 接口参数由 {@link XApiDocumentResolver} 提供。
 *
 * <p>HTTP 客户端不挂日志拦截器，避免 auth_token 和 ct0 写入日志。
 */
@Component
@RequiredArgsConstructor
public class XClient {

    /**
     * 访问 x.com 页面换取 ct0、下载媒体时使用的浏览器 User-Agent
     */
    private static final String BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/156.0.0.0 Safari/537.36";

    private static final String HOME_URL = "https://x.com";

    private static final String TYPEAHEAD_URL = "https://x.com/i/api/1.1/search/typeahead.json";

    /**
     * X 在 GraphQL errors 里表示未登录的错误名
     */
    private static final String AUTHENTICATION_ERROR = "AuthenticationError";

    /**
     * 查用户的 fieldToggles，按 RSSHub 的写法
     */
    private static final Map<String, Object> USER_FIELD_TOGGLES = ImmutableMap.of("withAuxiliaryUserLabels", false);

    private static final String AUTH_TOKEN_COOKIE = "auth_token";

    private static final String CSRF_TOKEN_COOKIE = "ct0";

    private static final DateTimeFormatter RATE_LIMIT_RESET_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int ERROR_BODY_MAX_LENGTH = 300;

    private final NewsProperties newsProperties;

    private final XApiDocumentResolver apiDocumentResolver;

    /**
     * auth_token → ct0
     */
    private final Map<String, String> csrfTokenByAuthToken = new ConcurrentHashMap<>();

    private OkHttpClient httpClient;

    /**
     * 按 {@code news.x.proxy-host / proxy-port} 创建走代理的 HTTP 客户端。
     */
    @PostConstruct
    public void initHttpClient() {
        NewsProperties.XChannel x = newsProperties.getX();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (StringUtils.isNotBlank(x.getProxyHost())) {
            builder.proxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(x.getProxyHost(), x.getProxyPort())));
        }
        httpClient = builder
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 查询 auth_token 当前登录的账号（GraphQL Viewer）。variables 和 fieldToggles 与 X 网页端的 Viewer 请求一致。
     *
     * @param authToken X 的 auth_token
     * @return GraphQL 响应，登录账号在 {@code data.viewer.user_results.result}
     * @throws ChannelLoginExpiredException auth_token 已失效
     */
    public JsonNode getViewer(String authToken) {
        Map<String, Object> variables = ImmutableMap.of("withCommunitiesMemberships", true);
        Map<String, Object> fieldToggles = ImmutableMap.of("isDelegate", false, "withAuxiliaryUserLabels", false);
        return executeGraphql(authToken, XGraphqlOperation.VIEWER, variables, fieldToggles);
    }

    /**
     * 按 @用户名查用户。
     *
     * @param authToken  X 的 auth_token
     * @param screenName 不带 @ 的用户名
     * @return GraphQL 响应；用户不存在时 {@code data} 下没有 {@code user}
     * @throws ChannelLoginExpiredException auth_token 已失效
     */
    public JsonNode getUserByScreenName(String authToken, String screenName) {
        Map<String, Object> variables = ImmutableMap.of("screen_name", screenName, "withSafetyModeUserFields", true);
        return executeGraphql(authToken, XGraphqlOperation.USER_BY_SCREEN_NAME, variables, USER_FIELD_TOGGLES);
    }

    /**
     * 按关键词搜索账号：网页端搜索框的用户联想（typeahead），按名称和用户名匹配。
     *
     * @param authToken X 的 auth_token
     * @param keyword   关键词，如名称或用户名
     * @return 联想结果，账号在 {@code users}，只有 id、名称、用户名、头像
     * @throws ChannelLoginExpiredException auth_token 已失效
     */
    public JsonNode searchUsersTypeahead(String authToken, String keyword) {
        HttpUrl url = HttpUrl.get(TYPEAHEAD_URL).newBuilder()
                .addQueryParameter("q", keyword)
                .addQueryParameter("src", "search_box")
                .addQueryParameter("result_type", "users")
                .build();
        return executeApi(authToken, url);
    }

    /**
     * 按用户 rest_id 查用户。
     *
     * @param authToken X 的 auth_token
     * @param restId    用户 rest_id
     * @return GraphQL 响应；用户不存在时 {@code data} 下没有 {@code user}
     * @throws ChannelLoginExpiredException auth_token 已失效
     */
    public JsonNode getUserByRestId(String authToken, String restId) {
        Map<String, Object> variables = ImmutableMap.of("userId", restId, "withSafetyModeUserFields", true);
        return executeGraphql(authToken, XGraphqlOperation.USER_BY_REST_ID, variables, USER_FIELD_TOGGLES);
    }

    /**
     * 取用户主页推文时间线的第一页。
     *
     * @param authToken X 的 auth_token
     * @param userId    用户 rest_id
     * @return GraphQL 响应，时间线在 {@code data.user.result.timeline.timeline.instructions}
     * @throws ChannelLoginExpiredException auth_token 已失效
     */
    public JsonNode getUserTweets(String authToken, String userId) {
        Map<String, Object> variables = ImmutableMap.<String, Object>builder()
                .put("userId", userId)
                .put("count", 20)
                .put("includePromotedContent", true)
                .put("withQuickPromoteEligibilityTweetFields", true)
                .put("withVoice", true)
                .put("withV2Timeline", true)
                .build();
        return executeGraphql(authToken, XGraphqlOperation.USER_TWEETS, variables, USER_FIELD_TOGGLES);
    }

    /**
     * 调用一个 GraphQL 接口：地址和 features 取自接口文档，variables 和 fieldToggles 由调用方按接口给出。
     */
    private JsonNode executeGraphql(String authToken, XGraphqlOperation operation, Map<String, Object> variables,
                                    Map<String, Object> fieldToggles) {
        XGraphqlEndpoint endpoint = apiDocumentResolver.resolve().endpointOf(operation);
        HttpUrl url = HttpUrl.get(endpoint.getUrl()).newBuilder()
                .addQueryParameter("variables", JacksonUtil.writeValueAsString(variables))
                .addQueryParameter("features", JacksonUtil.writeValueAsString(endpoint.getFeatures()))
                .addQueryParameter("fieldToggles", JacksonUtil.writeValueAsString(fieldToggles))
                .build();
        return executeApi(authToken, url);
    }

    /**
     * 带登录态请求接口，按状态码和 errors 判定失败。
     *
     * @throws ChannelLoginExpiredException 返回 401 / 403，或 errors 里有 AuthenticationError
     * @throws BusinessException            接口文档获取失败，返回 429 或其他非 2xx，或响应带其他 errors
     */
    private JsonNode executeApi(String authToken, HttpUrl url) {
        XApiDocument apiDocument = apiDocumentResolver.resolve();
        String csrfToken = getCsrfToken(authToken);
        Request request = new Request.Builder()
                .url(url)
                .header(HttpHeaders.USER_AGENT, apiDocument.getUserAgent())
                .header(HttpHeaders.ACCEPT, "*/*")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
                .header(HttpHeaders.AUTHORIZATION, apiDocument.getBearerToken())
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header("dnt", "1")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.REFERER, "https://x.com/")
                .header("x-twitter-active-user", "yes")
                .header("x-twitter-client-language", "en")
                .header("x-twitter-auth-type", "OAuth2Session")
                .header("x-csrf-token", csrfToken)
                .header(HttpHeaders.COOKIE, AUTH_TOKEN_COOKIE + "=" + authToken + "; " + CSRF_TOKEN_COOKIE + "=" + csrfToken)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            String body = Objects.requireNonNull(response.body(), "X 接口响应没有内容").string();
            int code = response.code();
            if (code == 401 || code == 403) {
                csrfTokenByAuthToken.remove(authToken);
                throw new ChannelLoginExpiredException(String.format("X 返回 %d，auth_token 已失效", code));
            }
            String rateLimitReset = Optional.ofNullable(response.header("x-rate-limit-reset"))
                    .map(reset -> "，限流重置时间 " + formatEpochSecond(reset))
                    .orElse("");
            if (code == 429) {
                throw new BusinessException("X 接口限流（HTTP 429）" + rateLimitReset);
            }
            if (!response.isSuccessful()) {
                throw new BusinessException(String.format("X 接口请求失败：HTTP %d%s，%s %s",
                        code, rateLimitReset, url.encodedPath(), StringUtils.abbreviate(body, ERROR_BODY_MAX_LENGTH)));
            }
            JsonNode root = JacksonUtil.readTree(body);
            checkErrors(root, url, authToken);
            return root;
        } catch (IOException e) {
            throw new UncheckedIOException("X 接口请求失败: " + url.encodedPath(), e);
        }
    }

    /**
     * 响应 200 但带 errors 时视为失败；X 对未登录的请求（如 Viewer）返回 200 加 AuthenticationError，按登录失效处理。
     */
    private void checkErrors(JsonNode root, HttpUrl url, String authToken) {
        JsonNode errors = root.path("errors");
        if (!errors.isArray() || errors.size() == 0) {
            return;
        }
        String messages = StreamSupport.stream(errors.spliterator(), false)
                .map(error -> error.path("message").asText())
                .collect(Collectors.joining("；"));
        boolean unauthenticated = StreamSupport.stream(errors.spliterator(), false)
                .anyMatch(error -> AUTHENTICATION_ERROR.equals(error.path("name").asText()));
        if (unauthenticated) {
            csrfTokenByAuthToken.remove(authToken);
            throw new ChannelLoginExpiredException("X 返回未登录（" + messages + "），auth_token 已失效");
        }
        throw new BusinessException(String.format("X 接口返回错误：%s，%s", url.encodedPath(), messages));
    }

    /**
     * 取 auth_token 对应的 ct0，内存里没有时访问 x.com 获取。
     */
    private String getCsrfToken(String authToken) {
        return csrfTokenByAuthToken.computeIfAbsent(authToken, this::requestCsrfToken);
    }

    /**
     * 带 auth_token cookie 访问 x.com（跟随跳转），从各次响应的 Set-Cookie 里取最后一个有效的 ct0。
     *
     * @throws ChannelLoginExpiredException x.com 没有下发 ct0，即 auth_token 已失效
     */
    private String requestCsrfToken(String authToken) {
        Cookie authCookie = new Cookie.Builder()
                .name(AUTH_TOKEN_COOKIE)
                .value(authToken)
                .domain("x.com")
                .path("/")
                .secure()
                .httpOnly()
                .build();
        List<Cookie> receivedCookies = Collections.synchronizedList(new ArrayList<>());
        CookieJar cookieJar = new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                receivedCookies.addAll(cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                return authCookie.matches(url) ? Collections.singletonList(authCookie) : Collections.emptyList();
            }
        };
        Request request = new Request.Builder()
                .url(HOME_URL)
                .header(HttpHeaders.USER_AGENT, BROWSER_USER_AGENT)
                .header(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
                .build();
        try (Response response = httpClient.newBuilder().cookieJar(cookieJar).build().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BusinessException(String.format("访问 x.com 失败：HTTP %d", response.code()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("访问 x.com 失败", e);
        }
        long now = System.currentTimeMillis();
        List<Cookie> csrfCookies = receivedCookies.stream()
                .filter(cookie -> CSRF_TOKEN_COOKIE.equals(cookie.name()))
                .collect(Collectors.toList());
        return Optional.of(csrfCookies)
                .filter(cookies -> !cookies.isEmpty())
                .map(cookies -> cookies.get(cookies.size() - 1))
                .filter(cookie -> StringUtils.isNotBlank(cookie.value()) && cookie.expiresAt() > now)
                .map(Cookie::value)
                .orElseThrow(() -> new ChannelLoginExpiredException("auth_token 已失效：x.com 未下发 ct0"));
    }

    /**
     * @param epochSecond 响应头里的秒级时间戳
     * @return 本地时间，如 2026-11-04 18:30:00
     */
    private static String formatEpochSecond(String epochSecond) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(epochSecond)), ZoneId.systemDefault())
                .format(RATE_LIMIT_RESET_FORMATTER);
    }
}
