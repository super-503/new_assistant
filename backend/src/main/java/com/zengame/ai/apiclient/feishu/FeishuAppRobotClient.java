package com.zengame.ai.apiclient.feishu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Suppliers;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 飞书自建应用机器人鉴权和单聊文本消息接口。 */
@Component
@RequiredArgsConstructor
public class FeishuAppRobotClient {

    private static final String TOKEN_URL = "https://accounts.feishu.cn/oauth/v3/token";
    private static final String MESSAGE_URL = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=open_id";

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

    private String getToken(Recipient target) {
        TokenRequest request = new TokenRequest("client_credentials", target.getAppId(), target.getAppSecret(),
                "im:message:send_as_bot");
        TokenReply reply = post(TOKEN_URL, request, TokenReply.class, "飞书应用鉴权失败");
        requireSuccess(Optional.ofNullable(reply).map(TokenReply::getCode), "飞书应用鉴权失败");
        return Optional.ofNullable(reply.getAccessToken()).filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new BusinessException("飞书未返回应用令牌"));
    }

    private void sendText(String token, String openId, String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        MessageRequest message = new MessageRequest(openId, "text", JacksonUtil.writeValueAsString(new Text(text)));
        MessageReply reply = post(MESSAGE_URL, new HttpEntity<>(message, headers), MessageReply.class, "飞书单聊发送失败");
        requireSuccess(Optional.ofNullable(reply).map(MessageReply::getCode), "飞书单聊发送失败");
    }

    private <T> T post(String url, Object request, Class<T> responseType, String failure) {
        try {
            return restTemplate.postForObject(url, request, responseType);
        } catch (RestClientResponseException e) {
            throw new BusinessException(failure + "，HTTP " + e.getRawStatusCode());
        } catch (RestClientException e) {
            throw new BusinessException(failure + "，" + e.getClass().getSimpleName());
        }
    }

    private void requireSuccess(Optional<Integer> code, String failure) {
        int value = code.orElse(-1);
        if (value != 0) {
            throw new BusinessException(failure + "，code=" + value);
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
    @ToString(onlyExplicitlyIncluded = true)
    private static class TokenRequest {
        @JsonProperty("grant_type")
        String grantType;
        @JsonProperty("client_id")
        String clientId;
        @JsonProperty("client_secret")
        String clientSecret;
        String scope;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenReply {
        Integer code;
        @ToString.Exclude
        @JsonProperty("access_token")
        String accessToken;
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
