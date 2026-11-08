package com.zengame.ai.business.news.channel.bilibili;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.zengame.ai.apiclient.bilibili.BilibiliClient;
import com.zengame.ai.apiclient.bilibili.BilibiliCorrespondPath;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliCookieRefreshResult;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliNavData;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliQrCodeData;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliQrPollResult;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliUserCard;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliVideo;
import com.zengame.ai.business.news.channel.ChannelLoginExpiredException;
import com.zengame.ai.business.news.channel.ChannelPlugin;
import com.zengame.ai.business.news.channel.CredentialRefreshable;
import com.zengame.ai.business.news.channel.QrLoginCapable;
import com.zengame.ai.business.news.channel.bilibili.bo.BilibiliCredential;
import com.zengame.ai.business.news.channel.bilibili.bo.BilibiliQrSession;
import com.zengame.ai.business.news.channel.bilibili.cover.BilibiliConvert;
import com.zengame.ai.business.news.channel.bo.ChannelCredential;
import com.zengame.ai.business.news.channel.bo.ChannelDescriptor;
import com.zengame.ai.business.news.channel.bo.CredentialVerifyResult;
import com.zengame.ai.business.news.channel.bo.FeedCursor;
import com.zengame.ai.business.news.channel.bo.FetchResult;
import com.zengame.ai.business.news.channel.bo.FetchedItem;
import com.zengame.ai.business.news.channel.bo.QrLoginPoll;
import com.zengame.ai.business.news.channel.bo.QrLoginStart;
import com.zengame.ai.business.news.channel.bo.SourceCandidate;
import com.zengame.ai.business.news.channel.bo.SourcePreview;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsCredentialMode;
import com.zengame.ai.business.news.domain.enums.NewsQrLoginStatus;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * B站渠道插件：订阅 UP 主投稿。
 *
 * <p>凭证为网页端扫码登录后的 cookie 和 refresh_token，每次拉取前按网页端流程检查并续期。
 * 搜索按昵称或 UID 查 UP 主；拉取 UP 主最新一页投稿，正文为视频简介，播放方式为官方内嵌播放器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BilibiliPlugin implements ChannelPlugin, QrLoginCapable, CredentialRefreshable {

    private static final ChannelDescriptor DESCRIPTOR = ChannelDescriptor.builder()
            .code("bilibili")
            .name("B站")
            .domain("bilibili.com")
            .defaultContentType(NewsContentType.VIDEO)
            .credentialMode(NewsCredentialMode.QR)
            .searchable(true)
            .searchHint("输入 UP 主昵称或 UID 搜索")
            .embedDomains(Collections.singletonList("player.bilibili.com"))
            .build();

    /**
     * 网页端登录二维码的有效期（秒）
     */
    private static final int QR_EXPIRE_SECONDS = 180;

    private static final int QR_IMAGE_SIZE = 300;

    private static final int QR_CODE_CONFIRMED = 0;

    private static final int QR_CODE_WAITING = 86101;

    private static final int QR_CODE_SCANNED = 86090;

    private static final int QR_CODE_EXPIRED = 86038;

    private final BilibiliClient bilibiliClient;

    @Override
    public ChannelDescriptor descriptor() {
        return DESCRIPTOR;
    }

    /**
     * 关键词为纯数字时按 UID 查用户卡片，否则按昵称搜索用户。
     */
    @Override
    public List<SourceCandidate> search(String keyword, ChannelCredential credential) {
        String cookieHeader = readCredential(credential).cookieHeader();
        String trimmed = StringUtils.trim(keyword);
        if (StringUtils.isNumeric(trimmed)) {
            return bilibiliClient.findUserCard(trimmed, cookieHeader)
                    .map(BilibiliConvert.INSTANCE::toCandidate)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        }
        return bilibiliClient.searchUsers(trimmed, cookieHeader).stream()
                .map(BilibiliConvert.INSTANCE::toCandidate)
                .collect(Collectors.toList());
    }

    /**
     * 来源卡片：UP 主信息和最新一页投稿。
     */
    @Override
    public SourcePreview preview(String sourceKey, ChannelCredential credential) {
        String cookieHeader = readCredential(credential).cookieHeader();
        BilibiliUserCard card = requireUserCard(sourceKey, cookieHeader);
        return SourcePreview.builder()
                .source(BilibiliConvert.INSTANCE.toCandidate(card))
                .contentType(NewsContentType.VIDEO)
                .recentItems(bilibiliClient.listLatestVideos(sourceKey, cookieHeader).stream()
                        .map(BilibiliConvert.INSTANCE::toRecentItem)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 取 UP 主最新一页投稿中投稿时间晚于拉取进度的视频。
     */
    @Override
    public FetchResult fetch(FeedCursor cursor, ChannelCredential credential) {
        String cookieHeader = readCredential(credential).cookieHeader();
        BilibiliUserCard author = requireUserCard(cursor.getSourceKey(), cookieHeader);
        List<BilibiliVideo> videos = bilibiliClient.listLatestVideos(cursor.getSourceKey(), cookieHeader);
        List<FetchedItem> items = videos.stream()
                .filter(video -> BilibiliConvert.INSTANCE.toDateTime(video.getCreated()).isAfter(cursor.getSince()))
                .map(video -> BilibiliConvert.INSTANCE.toFetchedItem(video, author))
                .collect(Collectors.toList());
        return FetchResult.builder().items(items).build();
    }

    /**
     * 取设备标识并生成网页端登录二维码，扫码会话里保存扫码登录密钥和设备 cookie。
     */
    @Override
    public QrLoginStart startQrLogin() {
        BilibiliQrSession session = new BilibiliQrSession(null, bilibiliClient.fetchDeviceCookies());
        BilibiliQrCodeData qrCode = bilibiliClient.generateQrCode();
        session.setQrcodeKey(qrCode.getQrcodeKey());
        return QrLoginStart.builder()
                .sessionState(JacksonUtil.writeValueAsString(session))
                .qrImage(QrCodeUtil.generateAsBase64(qrCode.getUrl(), new QrConfig(QR_IMAGE_SIZE, QR_IMAGE_SIZE), "png"))
                .expireSeconds(QR_EXPIRE_SECONDS)
                .build();
    }

    /**
     * 查询扫码结果；确认登录时把设备 cookie、登录 cookie 和 refresh_token 组装成凭证，登录账号取 nav 的昵称。
     *
     * @throws BusinessException 平台返回未知的扫码状态，或登录后 nav 仍为未登录
     */
    @Override
    public QrLoginPoll pollQrLogin(String sessionState) {
        BilibiliQrSession session = JacksonUtil.readValue(sessionState, BilibiliQrSession.class);
        BilibiliCredential deviceOnly = new BilibiliCredential(session.getDeviceCookies(), null);
        BilibiliQrPollResult result = bilibiliClient.pollQrCode(session.getQrcodeKey(), deviceOnly.cookieHeader());
        switch (result.getStatusCode()) {
            case QR_CODE_WAITING:
                return QrLoginPoll.builder().status(NewsQrLoginStatus.WAITING).sessionState(sessionState).build();
            case QR_CODE_SCANNED:
                return QrLoginPoll.builder().status(NewsQrLoginStatus.SCANNED).sessionState(sessionState).build();
            case QR_CODE_EXPIRED:
                return QrLoginPoll.builder().status(NewsQrLoginStatus.EXPIRED).build();
            case QR_CODE_CONFIRMED:
                return confirmQrLogin(session, result);
            default:
                throw new BusinessException(String.format("B站扫码登录返回未知状态：code=%d，%s",
                        result.getStatusCode(), result.getStatusMessage()));
        }
    }

    /**
     * 调 nav 校验登录态：已登录返回昵称，未登录返回失效。
     */
    @Override
    public CredentialVerifyResult verify(ChannelCredential credential) {
        BilibiliNavData nav = bilibiliClient.nav(readCredential(credential).cookieHeader());
        if (!nav.isLogin()) {
            return CredentialVerifyResult.invalid("B站登录已失效");
        }
        return CredentialVerifyResult.valid(nav.getUname());
    }

    /**
     * 调 cookie/info 检查是否需要刷新，需要时执行 {@link #refresh(ChannelCredential)}。
     *
     * @throws ChannelLoginExpiredException 检查或刷新的任一步失败，或返回未登录
     */
    @Override
    public Optional<ChannelCredential> refreshIfNeeded(ChannelCredential credential) {
        BilibiliCredential current = readCredential(credential);
        boolean needRefresh = refreshStep(() -> bilibiliClient.needRefreshCookie(current.cookieHeader(), current.csrf()));
        if (!needRefresh) {
            return Optional.empty();
        }
        return Optional.of(refresh(credential));
    }

    /**
     * 按网页端流程刷新凭证：生成 correspondPath → 取 refresh_csrf → cookie/refresh 换新 cookie 和 refresh_token →
     * 用新 cookie 调 confirm/refresh 作废旧 refresh_token。执行后旧凭证不再可用。
     *
     * @param credential 当前渠道凭证
     * @return 新凭证
     * @throws ChannelLoginExpiredException 任一步失败，或返回未登录
     */
    public ChannelCredential refresh(ChannelCredential credential) {
        BilibiliCredential current = readCredential(credential);
        return refreshStep(() -> {
            String correspondPath = BilibiliCorrespondPath.generate(System.currentTimeMillis());
            String refreshCsrf = bilibiliClient.fetchRefreshCsrf(correspondPath, current.cookieHeader());
            BilibiliCookieRefreshResult refreshed = bilibiliClient.refreshCookie(
                    current.cookieHeader(), current.csrf(), refreshCsrf, current.getRefreshToken());
            BilibiliCredential next = new BilibiliCredential(
                    BilibiliCredential.mergeCookies(current.getCookies(), refreshed.getCookies()),
                    refreshed.getRefreshToken());
            bilibiliClient.confirmRefresh(next.cookieHeader(), next.csrf(), current.getRefreshToken());
            log.info("B站凭证刷新完成");
            return ChannelCredential.from(next);
        });
    }

    /**
     * 组装扫码登录成功后的凭证，并用 nav 取登录账号。
     */
    private QrLoginPoll confirmQrLogin(BilibiliQrSession session, BilibiliQrPollResult result) {
        BilibiliCredential credential = new BilibiliCredential(
                BilibiliCredential.mergeCookies(session.getDeviceCookies(), result.getCookies()),
                result.getRefreshToken());
        BilibiliNavData nav = bilibiliClient.nav(credential.cookieHeader());
        if (!nav.isLogin()) {
            throw new BusinessException("B站扫码登录后仍为未登录状态");
        }
        return QrLoginPoll.builder()
                .status(NewsQrLoginStatus.CONFIRMED)
                .credential(ChannelCredential.from(credential))
                .account(nav.getUname())
                .build();
    }

    /**
     * @throws BusinessException UP 主不存在
     */
    private BilibiliUserCard requireUserCard(String mid, String cookieHeader) {
        return bilibiliClient.findUserCard(mid, cookieHeader)
                .orElseThrow(() -> new BusinessException("B站用户不存在：" + mid));
    }

    /**
     * @throws ChannelLoginExpiredException 渠道没有凭证
     */
    private static BilibiliCredential readCredential(ChannelCredential credential) {
        if (!credential.isPresent()) {
            throw new ChannelLoginExpiredException("B站未登录");
        }
        return credential.read(BilibiliCredential.class);
    }

    /**
     * 执行续期流程中的一步，任何失败都转为登录失效。
     */
    private static <T> T refreshStep(Supplier<T> step) {
        try {
            return step.get();
        } catch (RuntimeException e) {
            throw new ChannelLoginExpiredException("自动续期未成功：" + e.getMessage(), e);
        }
    }
}
