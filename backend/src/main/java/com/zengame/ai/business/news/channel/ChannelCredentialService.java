package com.zengame.ai.business.news.channel;

import com.zengame.ai.apiclient.feishu.FeishuRobotClient;
import com.zengame.ai.business.news.channel.bo.ChannelCredential;
import com.zengame.ai.business.news.channel.bo.ChannelDescriptor;
import com.zengame.ai.business.news.channel.bo.CredentialVerifyResult;
import com.zengame.ai.business.news.channel.bo.QrLoginPoll;
import com.zengame.ai.business.news.channel.bo.QrLoginStart;
import com.zengame.ai.business.news.domain.enums.NewsCredentialMode;
import com.zengame.ai.business.news.domain.enums.NewsCredentialStatus;
import com.zengame.ai.business.news.domain.enums.NewsQrLoginStatus;
import com.zengame.ai.business.news.feed.cover.ChannelCredentialConvert;
import com.zengame.ai.business.news.feed.domain.bo.QrLoginSessionBO;
import com.zengame.ai.business.news.feed.domain.vo.CredentialVerifyVO;
import com.zengame.ai.business.news.feed.domain.vo.QrLoginPollVO;
import com.zengame.ai.business.news.feed.domain.vo.QrLoginStartVO;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.RedisUtil;
import com.zengame.ai.repository.ai.mapper.NewsChannelMapper;
import com.zengame.ai.repository.ai.po.NewsChannelPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 渠道凭证服务：凭证存取、扫码登录会话、Token 保存、验证连接，以及凭证失效时的状态切换和飞书通知。
 *
 * <p>凭证只属于渠道，以插件自定义的不透明 JSON 存于 news_channel.credential_json，本服务只存取不解析。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelCredentialService {

    /**
     * 扫码会话在 Redis 的 key 前缀，完整 key 为 {@code news:qr:{sessionId}}
     */
    private static final String QR_SESSION_KEY_PREFIX = "news:qr:";

    /**
     * 飞书凭证失效通知：渠道名、原因、渠道设置链接
     */
    private static final String INVALID_NOTICE_TEMPLATE = "%s渠道登录已失效，原因：%s。点击处理：%s?channel=%s&settings=1";

    private final ChannelPluginRegistry channelPluginRegistry;

    private final NewsChannelMapper newsChannelMapper;

    private final RedisUtil redisUtil;

    private final FeishuRobotClient feishuRobotClient;

    private final NewsProperties newsProperties;

    /**
     * 把渠道凭证置为已失效并发飞书通知。只有凭证从有效切换为已失效的那一次发通知，已失效时不重复通知。
     *
     * @param channelCode 渠道标识
     * @param reason      失效原因，写进飞书通知
     * @return 本次是否把凭证置为已失效并发出了通知
     * @throws BusinessException     渠道不存在
     * @throws IllegalStateException 渠道无需凭证
     */
    public boolean markInvalid(String channelCode, String reason) {
        ChannelDescriptor descriptor = requireCredentialChannel(channelCode);
        int updated = newsChannelMapper.updateCredentialStatusByCode(
                channelCode, NewsCredentialStatus.VALID, NewsCredentialStatus.INVALID);
        if (updated == 0) {
            log.info("资讯渠道凭证已是失效状态，不重复通知, channel={}, reason={}", channelCode, reason);
            return false;
        }
        log.warn("资讯渠道凭证已失效, channel={}, reason={}", channelCode, reason);
        feishuRobotClient.sendRobotMessage(String.format(INVALID_NOTICE_TEMPLATE,
                descriptor.getName(), reason, newsProperties.getAdminUrl(), channelCode));
        return true;
    }

    /**
     * 保存渠道凭证，凭证状态置为有效，渠道恢复拉取。
     *
     * @param channelCode 渠道标识
     * @param credential  新凭证
     * @param account     登录账号；为 null 时保持原值
     * @throws BusinessException     渠道不存在
     * @throws IllegalStateException 渠道无需凭证，或凭证为空
     */
    public void saveCredential(String channelCode, ChannelCredential credential, String account) {
        requireCredentialChannel(channelCode);
        int updated = newsChannelMapper.updateCredentialByCode(channelCode, credential.getJson(),
                NewsCredentialStatus.VALID, account, LocalDateTime.now());
        if (updated != 1) {
            throw new BusinessException("资讯渠道不存在: " + channelCode);
        }
        log.info("资讯渠道凭证已保存, channel={}, account={}", channelCode, account);
    }

    /**
     * 读取渠道当前保存的凭证。
     *
     * @param channelCode 渠道标识
     * @return 渠道凭证；无需凭证的渠道为 {@link ChannelCredential#none()}
     * @throws BusinessException            渠道不存在
     * @throws ChannelLoginExpiredException 渠道需要凭证但尚未登录
     */
    public ChannelCredential loadCredential(String channelCode) {
        if (channelPluginRegistry.getPlugin(channelCode).descriptor().getCredentialMode() == NewsCredentialMode.NONE) {
            return ChannelCredential.none();
        }
        return Optional.ofNullable(newsChannelMapper.selectByCode(channelCode))
                .map(NewsChannelPO::getCredentialJson)
                .filter(StringUtils::isNotBlank)
                .map(ChannelCredential::of)
                .orElseThrow(() -> new ChannelLoginExpiredException("渠道尚未登录"));
    }

    /**
     * 发起扫码登录：向平台取二维码，扫码会话存入 Redis，有效期与二维码一致。
     *
     * @param channelCode 渠道标识
     * @return 扫码会话标识、二维码和有效期
     * @throws BusinessException 渠道不存在或不支持扫码登录
     */
    public QrLoginStartVO startQrLogin(String channelCode) {
        QrLoginStart start = channelPluginRegistry.getCapability(channelCode, QrLoginCapable.class).startQrLogin();
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        QrLoginSessionBO session = QrLoginSessionBO.builder()
                .channelCode(channelCode)
                .sessionState(start.getSessionState())
                .expireAtMillis(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(start.getExpireSeconds()))
                .build();
        redisUtil.setObject(qrSessionKey(sessionId), session, start.getExpireSeconds(), TimeUnit.SECONDS);
        return ChannelCredentialConvert.INSTANCE.toStartVO(sessionId, start);
    }

    /**
     * 轮询一次扫码结果。等待扫码、已扫码时写回会话状态；确认登录时保存凭证并结束会话；
     * 会话已不存在或二维码已过期时返回 EXPIRED。
     *
     * @param channelCode 渠道标识
     * @param sessionId   扫码会话标识
     * @return 扫码状态；CONFIRMED 时带登录账号
     * @throws BusinessException 渠道不存在、不支持扫码登录，或会话不属于该渠道
     */
    public QrLoginPollVO pollQrLogin(String channelCode, String sessionId) {
        QrLoginCapable plugin = channelPluginRegistry.getCapability(channelCode, QrLoginCapable.class);
        String key = qrSessionKey(sessionId);
        QrLoginSessionBO session = redisUtil.getObject(key, QrLoginSessionBO.class);
        if (session == null) {
            return ChannelCredentialConvert.INSTANCE.toPollVO(NewsQrLoginStatus.EXPIRED, null);
        }
        if (!channelCode.equals(session.getChannelCode())) {
            throw new BusinessException("扫码会话不属于该渠道");
        }
        QrLoginPoll poll = plugin.pollQrLogin(session.getSessionState());
        switch (poll.getStatus()) {
            case WAITING:
            case SCANNED:
                long remainingMillis = session.getExpireAtMillis() - System.currentTimeMillis();
                if (remainingMillis <= 0) {
                    redisUtil.delete(key);
                    return ChannelCredentialConvert.INSTANCE.toPollVO(NewsQrLoginStatus.EXPIRED, null);
                }
                session.setSessionState(Objects.requireNonNull(poll.getSessionState(), "扫码轮询未返回会话状态"));
                redisUtil.setObject(key, session, remainingMillis, TimeUnit.MILLISECONDS);
                return ChannelCredentialConvert.INSTANCE.toPollVO(poll.getStatus(), null);
            case CONFIRMED:
                saveCredential(channelCode, poll.getCredential(), poll.getAccount());
                redisUtil.delete(key);
                return ChannelCredentialConvert.INSTANCE.toPollVO(NewsQrLoginStatus.CONFIRMED, poll.getAccount());
            case EXPIRED:
                redisUtil.delete(key);
                return ChannelCredentialConvert.INSTANCE.toPollVO(NewsQrLoginStatus.EXPIRED, null);
            default:
                throw new IllegalStateException("未知的扫码状态: " + poll.getStatus());
        }
    }

    /**
     * 保存运营粘贴的 Token：插件组装凭证后先校验，可用才保存，登录账号取校验结果。
     *
     * @param channelCode 渠道标识
     * @param token       运营粘贴的 Token
     * @throws BusinessException 渠道不存在、不支持 Token，或 Token 校验不通过（带插件给出的原因）
     */
    public void saveToken(String channelCode, String token) {
        TokenCredential plugin = channelPluginRegistry.getCapability(channelCode, TokenCredential.class);
        ChannelCredential credential = plugin.createCredential(StringUtils.trim(token));
        CredentialVerifyResult result = plugin.verify(credential);
        if (!result.isValid()) {
            throw new BusinessException(result.getMessage());
        }
        saveCredential(channelCode, credential, result.getAccount());
    }

    /**
     * 校验运营输入框里的 Token：插件组装凭证后查询登录账号，不保存，不改变渠道凭证状态。
     *
     * @param channelCode 渠道标识
     * @param token       运营输入的 Token
     * @return 是否可用，可用时带登录账号，不可用时带失败原因
     * @throws BusinessException 渠道不存在或凭证方式不是 Token
     */
    public CredentialVerifyVO verifyToken(String channelCode, String token) {
        TokenCredential plugin = channelPluginRegistry.getCapability(channelCode, TokenCredential.class);
        ChannelCredential credential = plugin.createCredential(StringUtils.trim(token));
        return ChannelCredentialConvert.INSTANCE.toVerifyVO(plugin.verify(credential));
    }

    /**
     * 验证连接：用渠道当前保存的凭证查询登录账号。
     *
     * @param channelCode 渠道标识
     * @return 是否可用，可用时带登录账号，不可用时带失败原因
     * @throws BusinessException            渠道不存在或无需凭证
     * @throws ChannelLoginExpiredException 渠道尚未登录
     */
    public CredentialVerifyVO verifyCredential(String channelCode) {
        CredentialVerifiable plugin = channelPluginRegistry.getCapability(channelCode, CredentialVerifiable.class);
        return ChannelCredentialConvert.INSTANCE.toVerifyVO(plugin.verify(loadCredential(channelCode)));
    }

    /**
     * @param channelCode 渠道标识
     * @return 需要凭证的渠道的插件描述
     * @throws BusinessException     渠道不存在
     * @throws IllegalStateException 渠道无需凭证
     */
    private ChannelDescriptor requireCredentialChannel(String channelCode) {
        ChannelDescriptor descriptor = channelPluginRegistry.getPlugin(channelCode).descriptor();
        if (descriptor.getCredentialMode() == NewsCredentialMode.NONE) {
            throw new IllegalStateException("资讯渠道无需凭证: " + channelCode);
        }
        return descriptor;
    }

    private static String qrSessionKey(String sessionId) {
        return QR_SESSION_KEY_PREFIX + sessionId;
    }
}
