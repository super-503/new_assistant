package com.zengame.ai.apiclient.feishu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Suppliers;
import com.google.common.hash.Hashing;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 飞书自建应用的授权、用户身份和机器人单聊接口。 */
@Component
@RequiredArgsConstructor
public class FeishuAppRobotClient {

    private static final String TOKEN_URL = "https://accounts.feishu.cn/oauth/v3/token";
    private static final String AUTHORIZE_URL = "https://accounts.feishu.cn/open-apis/authen/v1/authorize";
    private static final String USER_TOKEN_URL = "https://open.feishu.cn/open-apis/authen/v2/oauth/token";
    private static final String USER_INFO_URL = "https://open.feishu.cn/open-apis/authen/v1/user_info";
    private static final String MESSAGE_URL = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=open_id";
    private static final Map<Integer, String> ERROR_HINTS = Map.of(
            20002, "请重新复制同一个应用的 App ID 和最新的 App Secret",
            20009, "请确认飞书自建应用已发布并安装",
            20048, "请核对飞书开发者后台的 App ID",
            20069, "请确认飞书应用已发布并启用",
            20130, "请核对 App ID、App Secret 和应用允许的客户端认证方式",
            20140, "请确认应用允许使用请求体中的 App Secret 鉴权（client_secret_post）",
            230013, "请把接收人加入飞书应用的可用范围并发布版本",
            99991672, "请确认应用已发布的应用身份权限包含「以应用的身份发消息」或「获取与发送单聊、群组消息」",
            99992351, "请核对接收人的 Open ID",
            99992361, "请在当前飞书应用下重新复制接收人的 Open ID"
    );

    private final RestTemplate restTemplate;

    /**
     * 创建一次投递任务使用的发送器。首次发送时获取应用令牌，本次任务内复用。
     * @param target 飞书应用凭证和单聊接收人
     * @return 发送单聊文本的函数，飞书拒绝请求时抛出 BusinessException
     */
    public Consumer<String> sender(Recipient target) {
        Supplier<String> token = Suppliers.memoize(() -> getToken(target));
        return text -> sendText(token.get(), target.getOpenId(), text);
    }

    /**
     * @param grant 当前飞书应用的接收人授权参数
     * @param state 管理端会话的授权标识
     * @return 飞书授权页地址
     */
    public String authorizationUrl(AuthorizationGrant grant, String state) {
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                Hashing.sha256().hashString(grant.getVerifier(), StandardCharsets.US_ASCII).asBytes());
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URL)
                .queryParam("client_id", grant.getCredentials().getAppId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", "{redirectUri}")
                .queryParam("state", state)
                .queryParam("code_challenge", challenge)
                .queryParam("code_challenge_method", "S256")
                .encode().buildAndExpand(grant.getRedirectUri()).toUriString();
    }

    /**
     * @param grant 发起授权时使用的应用和回调参数
     * @param code 飞书回传的一次性授权码
     * @return 授权人在该飞书应用下的身份信息
     * @throws BusinessException 授权码无效或飞书未返回有效的接收人
     */
    public AuthorizedUser authorizeUser(AuthorizationGrant grant, String code) {
        AppCredentials credentials = grant.getCredentials();
        UserTokenRequest request = new UserTokenRequest("authorization_code", credentials.getAppId(),
                credentials.getAppSecret(), code, grant.getRedirectUri(), grant.getVerifier());
        String token = readToken(USER_TOKEN_URL, request, "飞书用户授权失败");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        UserInfoReply reply = exchange(USER_INFO_URL, HttpMethod.GET, new HttpEntity<>(headers), UserInfoReply.class,
                "获取飞书用户信息失败");
        requireSuccess(Optional.ofNullable(reply).map(UserInfoReply::getCode), "获取飞书用户信息失败");
        return Optional.ofNullable(reply.getData()).filter(user -> StringUtils.startsWith(user.getOpenId(), "ou_"))
                .orElseThrow(() -> new BusinessException("飞书未返回接收人的 Open ID"));
    }

    private String getToken(Recipient target) {
        TokenRequest request = new TokenRequest("client_credentials", target.getAppId(), target.getAppSecret());
        return readToken(TOKEN_URL, request, "飞书应用鉴权失败");
    }

    private String readToken(String url, Object request, String failure) {
        TokenReply reply = exchange(url, HttpMethod.POST, new HttpEntity<>(request), TokenReply.class, failure);
        requireSuccess(Optional.ofNullable(reply).map(TokenReply::getCode), failure);
        return Optional.ofNullable(reply.getAccessToken()).filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new BusinessException("飞书未返回访问令牌"));
    }

    private void sendText(String token, String openId, String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        MessageRequest message = new MessageRequest(openId, "text", JacksonUtil.writeValueAsString(new Text(text)));
        MessageReply reply = exchange(MESSAGE_URL, HttpMethod.POST, new HttpEntity<>(message, headers),
                MessageReply.class, "飞书单聊发送失败");
        requireSuccess(Optional.ofNullable(reply).map(MessageReply::getCode), "飞书单聊发送失败");
    }

    private <T> T exchange(String url, HttpMethod method, HttpEntity<?> request, Class<T> responseType, String failure) {
        try {
            return restTemplate.exchange(url, method, request, responseType).getBody();
        } catch (RestClientResponseException e) {
            throw rejected(failure + "，HTTP " + e.getRawStatusCode(), responseCode(e));
        } catch (ResourceAccessException e) {
            throw new BusinessException(failure + "，请检查服务端到飞书的网络连接");
        } catch (RestClientException e) {
            throw new BusinessException(failure + "，" + e.getClass().getSimpleName());
        }
    }

    private void requireSuccess(Optional<Integer> code, String failure) {
        if (code.filter(value -> value == 0).isEmpty()) {
            throw rejected(failure, code.or(() -> Optional.of(-1)));
        }
    }

    private BusinessException rejected(String failure, Optional<Integer> code) {
        String number = code.map(value -> "，code=" + value).orElse("");
        String hint = code.map(ERROR_HINTS::get).map(value -> "。" + value).orElse("");
        return new BusinessException(failure + number + hint);
    }

    private Optional<Integer> responseCode(RestClientResponseException response) {
        try {
            return Optional.ofNullable(JacksonUtil.getObjectMapper().readTree(response.getResponseBodyAsString()))
                    .map(body -> body.path("code"))
                    .filter(JsonNode::isIntegralNumber)
                    .filter(JsonNode::canConvertToInt)
                    .map(JsonNode::intValue);
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    @Value
    public static class Recipient {
        String appId;
        @ToString.Exclude
        String appSecret;
        String openId;
    }

    @Value
    public static class AppCredentials {
        String appId;
        @ToString.Exclude
        String appSecret;
    }

    @Value
    public static class AuthorizationGrant {
        AppCredentials credentials;
        String redirectUri;
        @ToString.Exclude
        String verifier;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthorizedUser {
        @JsonProperty("open_id")
        private String openId;
        private String name;
        @JsonProperty("avatar_url")
        private String avatarUrl;
    }

    @Value
    @ToString(onlyExplicitlyIncluded = true)
    private static class TokenRequest {
        @JsonProperty("grant_type")
        String grantType;
        @JsonProperty("client_id")
        String clientId;
        @JsonProperty("client_secret")
        String clientSecret;
    }

    @Value
    @ToString(onlyExplicitlyIncluded = true)
    private static class UserTokenRequest {
        @JsonProperty("grant_type")
        String grantType;
        @JsonProperty("client_id")
        String clientId;
        @JsonProperty("client_secret")
        String clientSecret;
        String code;
        @JsonProperty("redirect_uri")
        String redirectUri;
        @JsonProperty("code_verifier")
        String codeVerifier;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenReply {
        Integer code;
        @ToString.Exclude
        @JsonProperty("access_token")
        String accessToken;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UserInfoReply {
        Integer code;
        AuthorizedUser data;
    }

    @Value
    private static class MessageRequest {
        @JsonProperty("receive_id")
        String receiveId;
        @JsonProperty("msg_type")
        String messageType;
        String content;
    }

    @Value
    private static class Text {
        String text;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MessageReply {
        Integer code;
    }
}
