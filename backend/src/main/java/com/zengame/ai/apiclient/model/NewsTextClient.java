package com.zengame.ai.apiclient.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.zengame.ai.business.news.processing.domain.bo.NewsAiModelReply;
import com.zengame.ai.business.news.setting.domain.bo.NewsAiModelConfig;
import com.zengame.ai.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 调用 OpenAI 兼容接口生成资讯加工字段。 */
@Component
@RequiredArgsConstructor
public class NewsTextClient {

    private final RestTemplate restTemplate;

    /**
     * 提交资讯加工提示词。
     *
     * @param config 模型地址、名称和密钥
     * @param prompt 发给模型的提示词
     * @return 模型输出和用于记录本次加工的标识
     * @throws BusinessException 调用失败或响应为空
     */
    public NewsAiModelReply complete(NewsAiModelConfig config, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Optional.ofNullable(config.getApiKey())
                .filter(StringUtils::isNotBlank)
                .ifPresent(headers::setBearerAuth);
        CompletionRequest request = new CompletionRequest(config.getModel(),
                List.of(new CompletionMessage("user", prompt)));

        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.postForEntity(URI.create(config.getModelUrl()), new HttpEntity<>(request, headers), JsonNode.class);
        } catch (RestClientResponseException e) {
            throw new BusinessException("模型接口返回 HTTP " + e.getRawStatusCode() + "，请检查模型配置");
        } catch (RestClientException e) {
            throw new BusinessException("资讯模型调用失败，请检查接口地址和网络连接");
        }
        JsonNode body = Optional.ofNullable(response.getBody())
                .orElseThrow(() -> new BusinessException("资讯模型响应为空"));
        String content = Optional.of(body.path("choices").path(0).path("message").path("content"))
                .filter(JsonNode::isTextual)
                .map(JsonNode::textValue)
                .filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new BusinessException("资讯模型没有返回正文"));
        String traceId = Optional.ofNullable(StringUtils.firstNonBlank(
                        response.getHeaders().getFirst("x-request-id"), body.path("id").textValue()))
                .orElseGet(() -> UUID.randomUUID().toString());
        return new NewsAiModelReply(traceId, content);
    }

    @Value
    private static class CompletionRequest {
        String model;
        List<CompletionMessage> messages;
    }

    @Value
    private static class CompletionMessage {
        String role;
        String content;
    }
}
