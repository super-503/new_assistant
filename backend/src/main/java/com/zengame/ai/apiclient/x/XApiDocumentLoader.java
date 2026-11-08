package com.zengame.ai.apiclient.x;

import com.fasterxml.jackson.databind.JsonNode;
import com.zengame.ai.apiclient.x.vo.XApiDocument;
import com.zengame.ai.apiclient.x.vo.XGraphqlEndpoint;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 下载并解析 TwitterInternalAPIDocument 的 API.json：该文档跟踪 X 网页端当前的 GraphQL queryId、features 和请求头。
 *
 * <p>经 jsDelivr 直连下载，不走 X 代理。下载失败、文档缺少插件用到的接口或请求头时直接抛异常，不使用任何内置取值。
 */
@Component
public class XApiDocumentLoader {

    /**
     * 文档地址，与 RSSHub lib/routes/twitter/api/web-api/gql-id-resolver.ts 读取的是同一份
     */
    private static final String DOCUMENT_URL = "https://cdn.jsdelivr.net/gh/fa0311/TwitterInternalAPIDocument@master/docs/json/API.json";

    private static final String BEARER_PREFIX = "Bearer ";

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * 下载最新的接口文档并解析。
     *
     * @return 接口参数
     * @throws BusinessException 下载失败，或文档缺少需要的接口或请求头
     */
    public XApiDocument load() {
        Request request = new Request.Builder().url(DOCUMENT_URL).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BusinessException(String.format("X 接口文档获取失败：HTTP %d", response.code()));
            }
            ResponseBody body = Objects.requireNonNull(response.body(), "X 接口文档响应没有内容");
            return parse(body.string());
        } catch (IOException e) {
            BusinessException exception = new BusinessException("X 接口文档获取失败：" + e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }

    /**
     * 解析接口文档，取出公开 Bearer、User-Agent 和 {@link XGraphqlOperation} 各接口的地址、queryId、features。
     *
     * @param documentJson API.json 的内容
     * @return 接口参数
     * @throws BusinessException 文档缺少需要的接口或请求头
     */
    public XApiDocument parse(String documentJson) {
        JsonNode document = JacksonUtil.readTree(documentJson);
        JsonNode header = document.path("header");
        String bearerToken = requiredText(header, "authorization", "header.authorization");
        if (!bearerToken.startsWith(BEARER_PREFIX)) {
            throw new BusinessException("X 接口文档的 header.authorization 不是 Bearer: " + StringUtils.abbreviate(bearerToken, 20));
        }
        return XApiDocument.builder()
                .bearerToken(bearerToken)
                .userAgent(requiredText(header, "user-agent", "header.user-agent"))
                .userByScreenName(parseEndpoint(document, XGraphqlOperation.USER_BY_SCREEN_NAME))
                .userByRestId(parseEndpoint(document, XGraphqlOperation.USER_BY_REST_ID))
                .userTweets(parseEndpoint(document, XGraphqlOperation.USER_TWEETS))
                .viewer(parseEndpoint(document, XGraphqlOperation.VIEWER))                .build();
    }

    /**
     * @throws BusinessException 文档里没有该接口，或接口缺少 url、queryId、features
     */
    private XGraphqlEndpoint parseEndpoint(JsonNode document, XGraphqlOperation operation) {
        String name = operation.getOperationName();
        JsonNode endpoint = document.path("graphql").path(name);
        if (!endpoint.isObject()) {
            throw new BusinessException("X 接口文档缺少 " + name);
        }
        JsonNode features = endpoint.path("features");
        if (!features.isObject()) {
            throw new BusinessException("X 接口文档缺少 " + name + ".features");
        }
        return XGraphqlEndpoint.builder()
                .url(requiredText(endpoint, "url", name + ".url"))
                .queryId(requiredText(endpoint, "queryId", name + ".queryId"))
                .features(features)
                .build();
    }

    /**
     * @throws BusinessException 字段不存在或为空
     */
    private static String requiredText(JsonNode node, String field, String path) {
        String value = node.path(field).asText();
        if (StringUtils.isBlank(value)) {
            throw new BusinessException("X 接口文档缺少 " + path);
        }
        return value;
    }
}
