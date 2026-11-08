package com.zengame.ai.apiclient.feishu;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuRobotClient {

    private final RestTemplate restTemplate;
    private final NewsProperties newsProperties;

    /**
     * 向配置的飞书群机器人发送资讯通知。
     *
     * @param message 通知正文
     */
    public void sendRobotMessage(String message) {
        String webhook = newsProperties.getFeishuWebhook();
        if (StringUtils.isBlank(webhook)) {
            return;
        }
        sendRobotMessage(webhook, message);
    }

    /**
     * 向指定的飞书群机器人发送文本通知。
     *
     * @param webhook 群机器人的 Webhook 地址
     * @param message 通知正文
     * @throws BusinessException 飞书未确认接收消息
     */
    public void sendRobotMessage(String webhook, String message) {
        RobotReply reply;
        try {
            reply = restTemplate.postForObject(webhook, new RobotMessage("text", new Text(message)), RobotReply.class);
        } catch (RestClientResponseException e) {
            throw new BusinessException("飞书机器人请求失败，HTTP " + e.getRawStatusCode());
        } catch (RestClientException | IllegalArgumentException e) {
            throw new BusinessException("飞书机器人请求失败，" + e.getClass().getSimpleName());
        }
        int code = Optional.ofNullable(reply).map(RobotReply::getCode).orElse(-1);
        if (code != 0) {
            throw new BusinessException("飞书机器人发送失败，code=" + code);
        }
        log.info("资讯飞书通知已发送");
    }

    @Value
    private static class RobotMessage {
        @JsonProperty("msg_type")
        String messageType;
        Text content;
    }

    @Value
    private static class Text {
        String text;
    }

    @lombok.Data
    private static class RobotReply {
        Integer code;
    }
}
