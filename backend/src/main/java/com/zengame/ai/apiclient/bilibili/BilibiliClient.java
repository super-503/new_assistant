package com.zengame.ai.apiclient.bilibili;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliCookie;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliCookieRefreshResult;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliNavData;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliQrCodeData;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliQrPollResult;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliSearchUser;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliUserCard;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliVideo;
import com.zengame.ai.business.news.channel.ChannelLoginExpiredException;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * B站网页端接口客户端：扫码登录、登录信息、cookie 刷新、用户卡片、用户搜索、投稿列表和媒体下载。
 *
 * <p>登录态通过 Cookie 请求头传入。接口返回 code -101（未登录）时抛 {@link ChannelLoginExpiredException}；
 * 其他非 0 的 code（如风控 -352、-412）和非 2xx 的 HTTP 状态抛 {@link BusinessException}，带上 code 和 message。
 * WBI 签名的 mixin key 缓存在内存里，跨天后重新从 nav 接口获取。
 */
@Slf4j
@Component
public class BilibiliClient {

    private static final String API_HOST = "https://api.bilibili.com";

    private static final String PASSPORT_HOST = "https://passport.bilibili.com";

    private static final String CORRESPOND_URL = "https://www.bilibili.com/correspond/1/";

    private static final String MAIN_REFERER = "https://www.bilibili.com/";

    private static final String SPACE_ORIGIN = "https://space.bilibili.com";

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    private static final String WEB_LOCATION = "1550101";

    private static final String REFRESH_CSRF_ELEMENT_ID = "1-name";

    private static final int CODE_OK = 0;

    private static final int CODE_NOT_LOGIN = -101;

    private static final int CODE_NOT_FOUND = -404;

    private static final int HTTP_NOT_FOUND = 404;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private String cachedMixinKey;

    private LocalDate cachedMixinKeyDate;

    /**
     * 取设备标识 buvid3、buvid4，作为扫码登录和后续请求的设备 cookie。
     *
     * @return buvid3、buvid4 两个 cookie
     */
    public List<BilibiliCookie> fetchDeviceCookies() {
        JsonNode data = requireData(execute(get(HttpUrl.get(API_HOST + "/x/frontend/finger/spi"), null, MAIN_REFERER),
                "获取设备标识"), "获取设备标识");
        return Arrays.asList(
                new BilibiliCookie("buvid3", data.path("b_3").asText()),
                new BilibiliCookie("buvid4", data.path("b_4").asText()));
    }

    /**
     * 生成网页端扫码登录的二维码。
     *
     * @return 二维码内容和扫码登录密钥
     */
    public BilibiliQrCodeData generateQrCode() {
        HttpUrl url = HttpUrl.get(PASSPORT_HOST + "/x/passport-login/web/qrcode/generate").newBuilder()
                .addQueryParameter("source", "main-fe-header")
                .build();
        String api = "生成登录二维码";
        return convert(requireData(execute(get(url, null, MAIN_REFERER), api), api), BilibiliQrCodeData.class);
    }

    /**
     * 查询一次扫码结果。
     *
     * @param qrcodeKey    扫码登录密钥
     * @param cookieHeader 设备 cookie
     * @return 扫码状态；登录成功时带登录 cookie 和 refresh_token
     */
    public BilibiliQrPollResult pollQrCode(String qrcodeKey, String cookieHeader) {
        HttpUrl url = HttpUrl.get(PASSPORT_HOST + "/x/passport-login/web/qrcode/poll").newBuilder()
                .addQueryParameter("qrcode_key", qrcodeKey)
                .build();
        String api = "查询扫码结果";
        ApiResponse response = execute(get(url, cookieHeader, MAIN_REFERER), api);
        JsonNode data = requireData(response, api);
        return new BilibiliQrPollResult(
                data.path("code").asInt(),
                data.path("message").asText(),
                StringUtils.trimToNull(data.path("refresh_token").asText()),
                response.cookies);
    }

    /**
     * 查询登录信息。未登录不抛异常，返回 {@code login=false}，WBI key 照常返回。
     *
     * @param cookieHeader 登录 cookie；匿名查询传 null
     * @return 登录信息
     */
    public BilibiliNavData nav(String cookieHeader) {
        String api = "查询登录信息";
        ApiResponse response = execute(get(HttpUrl.get(API_HOST + "/x/web-interface/nav"), cookieHeader, MAIN_REFERER), api);
        if (response.code != CODE_OK && response.code != CODE_NOT_LOGIN) {
            throw apiError(api, response);
        }
        return convert(response.data, BilibiliNavData.class);
    }

    /**
     * 检查登录 cookie 是否需要刷新。
     *
     * @param cookieHeader 登录 cookie
     * @param csrf         cookie 中的 bili_jct
     * @return 是否需要刷新
     */
    public boolean needRefreshCookie(String cookieHeader, String csrf) {
        HttpUrl url = HttpUrl.get(PASSPORT_HOST + "/x/passport-login/web/cookie/info").newBuilder()
                .addQueryParameter("csrf", csrf)
                .build();
        String api = "检查 cookie 刷新";
        return requireData(execute(get(url, cookieHeader, MAIN_REFERER), api), api).path("refresh").asBoolean();
    }

    /**
     * 请求 correspond 页面，取刷新 cookie 用的 refresh_csrf。
     *
     * @param correspondPath {@link BilibiliCorrespondPath#generate(long)} 的结果
     * @param cookieHeader   登录 cookie
     * @return refresh_csrf
     * @throws BusinessException correspondPath 已过期，或页面中没有 refresh_csrf
     */
    public String fetchRefreshCsrf(String correspondPath, String cookieHeader) {
        Request request = get(HttpUrl.get(CORRESPOND_URL + correspondPath), cookieHeader, MAIN_REFERER);
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == HTTP_NOT_FOUND) {
                throw new BusinessException("B站 correspondPath 已过期");
            }
            String html = readBody(response, "获取 refresh_csrf");
            return Optional.ofNullable(Jsoup.parse(html).getElementById(REFRESH_CSRF_ELEMENT_ID))
                    .map(Element::text)
                    .filter(StringUtils::isNotBlank)
                    .orElseThrow(() -> new BusinessException("B站 correspond 页面中没有 refresh_csrf"));
        } catch (IOException e) {
            throw new UncheckedIOException("B站获取 refresh_csrf 请求失败", e);
        }
    }

    /**
     * 刷新登录 cookie。
     *
     * @param cookieHeader 当前登录 cookie
     * @param csrf         当前 cookie 中的 bili_jct
     * @param refreshCsrf  correspond 页面取得的 refresh_csrf
     * @param refreshToken 当前 refresh_token
     * @return 新的登录 cookie 和新的 refresh_token
     */
    public BilibiliCookieRefreshResult refreshCookie(String cookieHeader, String csrf, String refreshCsrf, String refreshToken) {
        FormBody body = new FormBody.Builder()
                .add("csrf", csrf)
                .add("refresh_csrf", refreshCsrf)
                .add("source", "main_web")
                .add("refresh_token", refreshToken)
                .build();
        String api = "刷新 cookie";
        ApiResponse response = execute(post(HttpUrl.get(PASSPORT_HOST + "/x/passport-login/web/cookie/refresh"),
                body, cookieHeader), api);
        String newRefreshToken = requireData(response, api).path("refresh_token").asText();
        if (StringUtils.isBlank(newRefreshToken) || response.cookies.isEmpty()) {
            throw new BusinessException("B站刷新 cookie 未返回新的 cookie 或 refresh_token");
        }
        return new BilibiliCookieRefreshResult(newRefreshToken, response.cookies);
    }

    /**
     * 确认刷新，作废刷新前的 refresh_token。
     *
     * @param cookieHeader    刷新后的新登录 cookie
     * @param csrf            新 cookie 中的 bili_jct
     * @param oldRefreshToken 刷新前的 refresh_token
     */
    public void confirmRefresh(String cookieHeader, String csrf, String oldRefreshToken) {
        FormBody body = new FormBody.Builder()
                .add("csrf", csrf)
                .add("refresh_token", oldRefreshToken)
                .build();
        String api = "确认刷新 cookie";
        requireData(execute(post(HttpUrl.get(PASSPORT_HOST + "/x/passport-login/web/confirm/refresh"),
                body, cookieHeader), api), api);
    }

    /**
     * 按 UID 查用户卡片。
     *
     * @param mid          用户 ID
     * @param cookieHeader 登录 cookie
     * @return 用户卡片；用户不存在时为空
     */
    public Optional<BilibiliUserCard> findUserCard(String mid, String cookieHeader) {
        HttpUrl url = HttpUrl.get(API_HOST + "/x/web-interface/card").newBuilder()
                .addQueryParameter("mid", mid)
                .build();
        String api = "查询用户卡片";
        ApiResponse response = execute(get(url, cookieHeader, spaceReferer(mid)), api);
        if (response.code == CODE_NOT_FOUND) {
            return Optional.empty();
        }
        return Optional.of(convert(requireData(response, api).path("card"), BilibiliUserCard.class));
    }

    /**
     * 按关键词搜索用户，取第一页。
     *
     * @param keyword      昵称关键词
     * @param cookieHeader 登录 cookie
     * @return 搜索结果；搜不到时为空列表
     */
    public List<BilibiliSearchUser> searchUsers(String keyword, String cookieHeader) {
        Map<String, String> params = new TreeMap<>();
        params.put("search_type", "bili_user");
        params.put("keyword", keyword);
        params.put("page", "1");
        params.put("web_location", WEB_LOCATION);
        String api = "搜索用户";
        JsonNode data = requireData(execute(get(signedUrl("/x/web-interface/wbi/search/type", params),
                cookieHeader, MAIN_REFERER), api), api);
        return convertList(data.path("result"), new TypeReference<List<BilibiliSearchUser>>() {
        });
    }

    /**
     * 取 UP 主最新一页投稿，按发布时间倒序，每页 30 条。
     *
     * @param mid          UP 主用户 ID
     * @param cookieHeader 登录 cookie
     * @return 投稿列表；没有投稿时为空列表
     */
    public List<BilibiliVideo> listLatestVideos(String mid, String cookieHeader) {
        Map<String, String> params = new TreeMap<>(BilibiliDmParams.generate());
        params.put("mid", mid);
        params.put("ps", "30");
        params.put("tid", "0");
        params.put("pn", "1");
        params.put("keyword", "");
        params.put("order", "pubdate");
        params.put("platform", "web");
        params.put("web_location", WEB_LOCATION);
        params.put("order_avoided", "true");
        Request request = get(signedUrl("/x/space/wbi/arc/search", params), cookieHeader, spaceReferer(mid))
                .newBuilder()
                .header("Origin", SPACE_ORIGIN)
                .build();
        String api = "查询投稿列表";
        JsonNode data = requireData(execute(request, api), api);
        return convertList(data.path("list").path("vlist"), new TypeReference<List<BilibiliVideo>>() {
        });
    }

    /**
     * 拼出带 WBI 签名的接口地址。
     */
    private HttpUrl signedUrl(String path, Map<String, String> params) {
        String query = BilibiliWbiSign.signQuery(params, currentMixinKey(), Instant.now().getEpochSecond());
        return HttpUrl.get(API_HOST + path + "?" + query);
    }

    /**
     * 当天的 mixin key，跨天后从匿名 nav 接口重新获取。
     */
    private synchronized String currentMixinKey() {
        LocalDate today = LocalDate.now();
        if (StringUtils.isEmpty(cachedMixinKey) || !today.equals(cachedMixinKeyDate)) {
            BilibiliNavData.WbiImg wbiImg = nav(null).getWbiImg();
            cachedMixinKey = BilibiliWbiSign.mixinKey(keyOf(wbiImg.getImgUrl()), keyOf(wbiImg.getSubUrl()));
            cachedMixinKeyDate = today;
        }
        return cachedMixinKey;
    }

    /**
     * WBI key 图片地址的文件名（不含扩展名）。
     */
    private static String keyOf(String imageUrl) {
        return StringUtils.substringBefore(StringUtils.substringAfterLast(imageUrl, "/"), ".");
    }

    private static String spaceReferer(String mid) {
        return SPACE_ORIGIN + "/" + mid;
    }

    private static Request get(HttpUrl url, String cookieHeader, String referer) {
        return baseRequest(url, cookieHeader, referer).get().build();
    }

    private static Request post(HttpUrl url, FormBody body, String cookieHeader) {
        return baseRequest(url, cookieHeader, MAIN_REFERER).post(body).build();
    }

    private static Request.Builder baseRequest(HttpUrl url, String cookieHeader, String referer) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", referer);
        Optional.ofNullable(cookieHeader).filter(StringUtils::isNotBlank).ifPresent(cookie -> builder.header("Cookie", cookie));
        return builder;
    }

    /**
     * 执行 JSON 接口请求，解析出 code、message、data 和 Set-Cookie。
     */
    private ApiResponse execute(Request request, String api) {
        try (Response response = httpClient.newCall(request).execute()) {
            JsonNode root = JacksonUtil.readTree(readBody(response, api));
            List<BilibiliCookie> cookies = Cookie.parseAll(request.url(), response.headers()).stream()
                    .filter(cookie -> cookie.expiresAt() > System.currentTimeMillis())
                    .map(cookie -> new BilibiliCookie(cookie.name(), cookie.value()))
                    .collect(Collectors.toList());
            return new ApiResponse(root.path("code").asInt(), root.path("message").asText(), root.path("data"), cookies);
        } catch (IOException e) {
            throw new UncheckedIOException("B站" + api + "请求失败", e);
        }
    }

    private static String readBody(Response response, String api) throws IOException {
        if (!response.isSuccessful()) {
            throw new BusinessException(String.format("B站%s失败：HTTP %d", api, response.code()));
        }
        return Optional.ofNullable(response.body())
                .orElseThrow(() -> new BusinessException("B站" + api + "响应为空"))
                .string();
    }

    /**
     * 校验接口 code 并取 data。
     *
     * @throws ChannelLoginExpiredException code 为 -101（未登录）
     * @throws BusinessException            其他非 0 的 code
     */
    private static JsonNode requireData(ApiResponse response, String api) {
        if (response.code == CODE_NOT_LOGIN) {
            throw new ChannelLoginExpiredException("B站登录已失效");
        }
        if (response.code != CODE_OK) {
            throw apiError(api, response);
        }
        return response.data;
    }

    private static BusinessException apiError(String api, ApiResponse response) {
        return new BusinessException(String.format("B站%s失败：code=%d，%s", api, response.code, response.message));
    }

    private static <T> T convert(JsonNode node, Class<T> type) {
        return JacksonUtil.getObjectMapper().convertValue(node, type);
    }

    private static <T> List<T> convertList(JsonNode node, TypeReference<List<T>> type) {
        if (!node.isArray()) {
            return Collections.emptyList();
        }
        return JacksonUtil.getObjectMapper().convertValue(node, type);
    }

    /**
     * JSON 接口的统一响应：code、message、data 和响应下发的 cookie。
     */
    private static final class ApiResponse {

        private final int code;

        private final String message;

        private final JsonNode data;

        private final List<BilibiliCookie> cookies;

        private ApiResponse(int code, String message, JsonNode data, List<BilibiliCookie> cookies) {
            this.code = code;
            this.message = message;
            this.data = data;
            this.cookies = cookies;
        }
    }
}
