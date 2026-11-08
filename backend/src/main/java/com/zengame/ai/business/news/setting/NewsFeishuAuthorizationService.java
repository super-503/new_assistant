package com.zengame.ai.business.news.setting;

import com.zengame.ai.apiclient.feishu.FeishuAppRobotClient;
import com.zengame.ai.business.news.setting.cover.NewsFeishuNotificationSettingConvert;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuAppRequest;
import com.zengame.ai.business.news.setting.domain.vo.NewsFeishuAuthorizationStartVO;
import com.zengame.ai.business.news.setting.domain.vo.NewsFeishuAuthorizedUserVO;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** 在当前管理员会话内识别飞书授权人，用于填写日报接收人。 */
@Service
@RequiredArgsConstructor
public class NewsFeishuAuthorizationService {

    private static final String SESSION_KEY = NewsFeishuAuthorizationService.class.getName();
    private static final Duration VALIDITY = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final NewsFeishuNotificationSettingService notificationSettings;
    private final FeishuAppRobotClient appClient;
    private final NewsProperties properties;

    /**
     * 使用通知表单中的应用发起一次飞书接收人授权。
     * @param request 通知应用凭证
     * @param session 当前管理员会话
     * @return 授权页地址和查询标识
     * @throws BusinessException 应用密钥或回调地址无效
     */
    public NewsFeishuAuthorizationStartVO start(NewsFeishuAppRequest request, HttpSession session) {
        String state = randomValue();
        FeishuAppRobotClient.AuthorizationGrant grant = new FeishuAppRobotClient.AuthorizationGrant(
                notificationSettings.authorizationCredentials(request), redirectUri(), randomValue());
        String url = appClient.authorizationUrl(grant, state);
        session.setAttribute(SESSION_KEY, new AuthorizationAttempt(state, grant));
        return new NewsFeishuAuthorizationStartVO(state, url, VALIDITY.getSeconds());
    }

    /**
     * 校验回调并识别授权人。每次授权只交换一次授权码。
     * @param session 发起授权的管理员会话
     * @param state 飞书回传的授权标识
     * @param code 一次性授权码，取消时为空
     * @param error 飞书回传的授权拒绝标记
     * @throws BusinessException 会话、授权标识或有效期不匹配
     */
    public void complete(HttpSession session, String state, String code, String error) {
        AuthorizationAttempt attempt = requireAttempt(session, state);
        FeishuAppRobotClient.AuthorizationGrant grant = Optional.ofNullable(attempt.grant.getAndSet(null))
                .orElseThrow(() -> new BusinessException("本次飞书授权已处理"));
        if (StringUtils.isNotBlank(error) || StringUtils.isBlank(code)) {
            attempt.outcome = new Outcome(null, "飞书授权已取消或未完成");
            return;
        }

        try {
            FeishuAppRobotClient.AuthorizedUser user = appClient.authorizeUser(grant, code);
            attempt.outcome = new Outcome(NewsFeishuNotificationSettingConvert.INSTANCE.toAuthorizedUser(user), null);
        } catch (BusinessException e) {
            attempt.outcome = new Outcome(null, e.getMessage());
        }
    }

    /**
     * @param session 发起授权的管理员会话
     * @param state 本次授权的查询标识
     * @return 已授权的接收人；等待授权时为空
     * @throws BusinessException 会话过期、授权取消或飞书接口失败
     */
    public NewsFeishuAuthorizedUserVO result(HttpSession session, String state) {
        return Optional.ofNullable(requireAttempt(session, state).outcome).map(outcome -> {
            if (StringUtils.isNotBlank(outcome.getError())) {
                throw new BusinessException(outcome.getError());
            }
            return outcome.getUser();
        }).orElse(null);
    }

    /** @return 与本次回调匹配且仍有效的授权会话 */
    private AuthorizationAttempt requireAttempt(HttpSession session, String state) {
        AuthorizationAttempt attempt = Optional.ofNullable((AuthorizationAttempt) session.getAttribute(SESSION_KEY))
                .filter(value -> value.state.equals(state))
                .orElseThrow(() -> new BusinessException("飞书授权会话不匹配，请重新授权"));
        if (!Instant.now().isBefore(attempt.expiresAt)) {
            attempt.grant.set(null);
            throw new BusinessException("飞书授权已过期，请重新授权");
        }
        return attempt;
    }

    /** @return 飞书开发者后台登记的回调地址 */
    private String redirectUri() {
        URI uri;
        try {
            uri = URI.create(StringUtils.trimToEmpty(properties.getFeishuAuthorizationRedirectUri()));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("请检查飞书授权回调地址配置");
        }
        if (!StringUtils.equalsAnyIgnoreCase(uri.getScheme(), "http", "https") || uri.getHost() == null
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new BusinessException("请配置完整的飞书授权回调地址");
        }
        return uri.toASCIIString();
    }

    private static String randomValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static final class AuthorizationAttempt {
        private final String state;
        private final Instant expiresAt = Instant.now().plus(VALIDITY);
        private final AtomicReference<FeishuAppRobotClient.AuthorizationGrant> grant;
        private volatile Outcome outcome;

        private AuthorizationAttempt(String state, FeishuAppRobotClient.AuthorizationGrant grant) {
            this.state = state;
            this.grant = new AtomicReference<>(grant);
        }
    }

    @Value
    private static class Outcome {
        NewsFeishuAuthorizedUserVO user;
        String error;
    }
}
