package com.zengame.ai.business.news.channel.wechatmp;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.zengame.ai.apiclient.wechatmp.WechatMpClient;
import com.zengame.ai.apiclient.wechatmp.WereadClient;
import com.zengame.ai.apiclient.wechatmp.vo.WechatMpCookie;
import com.zengame.ai.business.news.channel.ChannelLoginExpiredException;
import com.zengame.ai.business.news.channel.ChannelPlugin;
import com.zengame.ai.business.news.channel.QrLoginCapable;
import com.zengame.ai.business.news.channel.bo.ChannelCredential;
import com.zengame.ai.business.news.channel.bo.ChannelDescriptor;
import com.zengame.ai.business.news.channel.bo.CredentialVerifyResult;
import com.zengame.ai.business.news.channel.bo.FeedCursor;
import com.zengame.ai.business.news.channel.bo.FetchResult;
import com.zengame.ai.business.news.channel.bo.QrLoginPoll;
import com.zengame.ai.business.news.channel.bo.QrLoginStart;
import com.zengame.ai.business.news.channel.bo.SourceCandidate;
import com.zengame.ai.business.news.channel.bo.SourcePreview;
import com.zengame.ai.business.news.channel.wechatmp.bo.WechatMpCredential;
import com.zengame.ai.business.news.channel.wechatmp.cover.WechatMpConvert;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsCredentialMode;
import com.zengame.ai.business.news.domain.enums.NewsQrLoginStatus;
import com.zengame.ai.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通过微信读书订阅公众号，每次拉取每个公众号的最新一篇文章。
 *
 * <p>微信读书登录态不自动续期：失效时按未登录处理，由运营重新扫码。
 */
@Component
@RequiredArgsConstructor
public class WechatMpPlugin implements ChannelPlugin, QrLoginCapable {

    private static final String BOOK_PREFIX = "MP_WXS_";
    private static final Pattern BOOK_ID = Pattern.compile("MP_WXS_[0-9]+");
    private static final Pattern BIZ = Pattern.compile("\\b(?:var\\s+|window\\.)biz\\s*=\\s*['\"]([A-Za-z0-9+/=]+)['\"]");
    private static final Pattern PUBLISH_TIMESTAMP = Pattern.compile("\\bvar\\s+ct\\s*=\\s*['\"]([0-9]{10})['\"]");
    private static final Pattern ARTICLE_TOKEN = Pattern.compile("[A-Za-z0-9_-]+");
    private static final String LOGIN_REQUIRED = "请扫码登录微信读书";
    private static final int QR_IMAGE_SIZE = 240;
    private static final int QR_EXPIRE_SECONDS = 300;

    private final WereadClient wereadClient;
    private final WechatMpClient wechatMpClient;

    @Override
    public ChannelDescriptor descriptor() {
        return ChannelDescriptor.builder()
                .code("wechat_mp")
                .name("公众号")
                .domain("mp.weixin.qq.com")
                .defaultContentType(NewsContentType.ARTICLE)
                .credentialMode(NewsCredentialMode.QR)
                .searchable(true)
                .searchHint("粘贴该公众号任意一篇文章的链接")
                .embedDomains(List.of("v.qq.com"))
                .build();
    }

    /**
     * 从公众号文章链接识别来源，并返回微信读书里的公众号资料。
     *
     * @param keyword 公众号文章链接
     * @param credential 微信读书登录凭证
     * @return 已识别的公众号
     */
    @Override
    public List<SourceCandidate> search(String keyword, ChannelCredential credential) {
        List<WechatMpCookie> cookies = readCookies(credential);
        String url = WechatMpArticleUrl.canonicalize(StringUtils.trim(keyword));
        String biz = articleBiz(url);
        String bookId = BOOK_PREFIX + decodeBiz(biz);
        return List.of(source(requireBookId(bookId), cookies));
    }

    /**
     * 预览公众号资料及最新一篇文章。
     *
     * @param sourceKey 微信读书公众号编号
     * @param credential 微信读书登录凭证
     * @return 公众号及最新文章
     */
    @Override
    public SourcePreview preview(String sourceKey, ChannelCredential credential) {
        String bookId = requireBookId(sourceKey);
        List<WechatMpCookie> cookies = readCookies(credential);
        SourceCandidate source = source(bookId, cookies);
        WereadClient.Cover cover = withLogin(() -> wereadClient.cover(cookies, bookId));
        Document document = withLogin(() -> wereadClient.content(cookies, cover.getReviewId()));
        return SourcePreview.builder()
                .source(source)
                .contentType(NewsContentType.ARTICLE)
                .recentItems(List.of(WechatMpConvert.INSTANCE.toRecentItem(cover, publishTime(document))))
                .build();
    }

    /**
     * 拉取发布时间晚于订阅进度的最新文章，并以原文短链作为条目标识。
     *
     * @param cursor 微信读书公众号编号及拉取进度
     * @param credential 微信读书登录凭证
     * @return 最新文章，或没有更新时的空列表
     */
    @Override
    public FetchResult fetch(FeedCursor cursor, ChannelCredential credential) {
        String bookId = requireBookId(cursor.getSourceKey());
        List<WechatMpCookie> cookies = readCookies(credential);
        WereadClient.Cover cover = withLogin(() -> wereadClient.cover(cookies, bookId));
        String url = originalUrl(bookId, cover.getReviewId());
        Document document = withLogin(() -> wereadClient.content(cookies, cover.getReviewId()));
        LocalDateTime published = publishTime(document);
        if (!published.isAfter(cursor.getSince())) {
            return FetchResult.builder().items(List.of()).build();
        }
        Element content = document.getElementById("js_content");
        String html = WechatMpArticleParser.fixContent(content, url);
        return FetchResult.builder()
                .items(List.of(WechatMpConvert.INSTANCE.toFetchedItem(cover, url, html, published)))
                .build();
    }

    @Override
    public CredentialVerifyResult verify(ChannelCredential credential) {
        try {
            List<WechatMpCookie> cookies = readCookies(credential);
            return CredentialVerifyResult.valid(withLogin(() -> wereadClient.account(cookies)));
        } catch (ChannelLoginExpiredException e) {
            return CredentialVerifyResult.invalid(e.getMessage());
        }
    }

    @Override
    public QrLoginStart startQrLogin() {
        String uid = wereadClient.startLogin();
        return QrLoginStart.builder()
                .sessionState(uid)
                .qrImage(QrCodeUtil.generateAsBase64(wereadClient.qrUrl(uid),
                        new QrConfig(QR_IMAGE_SIZE, QR_IMAGE_SIZE), "png"))
                .expireSeconds(QR_EXPIRE_SECONDS)
                .build();
    }

    @Override
    public QrLoginPoll pollQrLogin(String sessionState) {
        WereadClient.LoginPoll result = wereadClient.pollLogin(sessionState);
        if (result.getState() == WereadClient.LoginState.CONFIRMED) {
            List<WechatMpCookie> cookies = result.getCookies();
            return QrLoginPoll.builder()
                    .status(NewsQrLoginStatus.CONFIRMED)
                    .credential(wrapCookies(cookies))
                    .account(withLogin(() -> wereadClient.account(cookies)))
                    .build();
        }
        NewsQrLoginStatus status = result.getState() == WereadClient.LoginState.SCANNED
                ? NewsQrLoginStatus.SCANNED : NewsQrLoginStatus.WAITING;
        return QrLoginPoll.builder().status(status).sessionState(sessionState).build();
    }

    /**
     * 查询公众号资料，使用微信读书编号作为订阅标识。
     */
    private SourceCandidate source(String bookId, List<WechatMpCookie> cookies) {
        return WechatMpConvert.INSTANCE.toSourceCandidate(bookId, withLogin(() -> wereadClient.bookInfo(cookies, bookId)));
    }

    /**
     * 长链自带公众号编号；短链通过公开文章页识别公众号。
     */
    private String articleBiz(String url) {
        String biz = okhttp3.HttpUrl.get(url).queryParameter("__biz");
        if (StringUtils.isNotBlank(biz)) {
            return biz;
        }
        return requireArticleField(wechatMpClient.fetchArticlePage(url), BIZ, "公众号编号");
    }

    private String decodeBiz(String biz) {
        try {
            return new String(Base64.getDecoder().decode(biz), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("公众号文章链接中的公众号编号无效");
        }
    }

    /**
     * 以原文发布时间推进游标，不以采集时间替代发布时间。
     */
    private LocalDateTime publishTime(Document document) {
        long epochSecond = Long.parseLong(requireArticleField(document, PUBLISH_TIMESTAMP, "发布时间"));
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneId.systemDefault());
    }

    private String requireArticleField(Document document, Pattern pattern, String label) {
        return document.select("script").stream()
                .map(script -> pattern.matcher(script.data()))
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .findFirst()
                .orElseThrow(() -> new BusinessException("文章页缺少" + label));
    }

    private String originalUrl(String bookId, String reviewId) {
        String prefix = bookId + "_";
        if (!StringUtils.startsWith(reviewId, prefix)) {
            throw new BusinessException("微信读书文章编号不属于该公众号");
        }
        String token = reviewId.substring(prefix.length()).replace('~', '_');
        if (!ARTICLE_TOKEN.matcher(token).matches()) {
            throw new BusinessException("微信读书文章编号缺少有效的原文短链");
        }
        return "https://mp.weixin.qq.com/s/" + token;
    }

    private String requireBookId(String bookId) {
        if (StringUtils.isBlank(bookId) || !BOOK_ID.matcher(bookId).matches()) {
            throw new BusinessException("无效的微信读书公众号编号，请粘贴公众号文章链接添加订阅源");
        }
        return bookId;
    }

    private List<WechatMpCookie> readCookies(ChannelCredential credential) {
        if (!credential.isPresent()) {
            throw new ChannelLoginExpiredException(LOGIN_REQUIRED);
        }
        List<WechatMpCookie> cookies = credential.read(WechatMpCredential.class).getCookies();
        if (CollectionUtils.isEmpty(cookies)
                || cookies.stream().noneMatch(cookie -> "wr_skey".equals(cookie.getName()))) {
            throw new ChannelLoginExpiredException(LOGIN_REQUIRED);
        }
        return cookies;
    }

    private ChannelCredential wrapCookies(List<WechatMpCookie> cookies) {
        return ChannelCredential.from(WechatMpCredential.builder().cookies(cookies).build());
    }

    /**
     * 将平台会话失效交给资讯渠道的凭证状态处理。
     */
    private <T> T withLogin(Supplier<T> request) {
        try {
            return request.get();
        } catch (WereadClient.SessionExpiredException e) {
            throw new ChannelLoginExpiredException(e.getMessage(), e);
        }
    }
}
