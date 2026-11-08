package com.zengame.ai.apiclient.x.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zengame.ai.apiclient.x.XGraphqlOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * X 网页端接口参数：公开 Bearer、网页端 User-Agent 和插件用到的 GraphQL 接口，取自 TwitterInternalAPIDocument 的 API.json。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XApiDocument {

    /**
     * 请求头 authorization 的取值，形如 "Bearer AAAA..."
     */
    private String bearerToken;

    /**
     * 网页端请求头 user-agent
     */
    private String userAgent;

    private XGraphqlEndpoint userByScreenName;

    private XGraphqlEndpoint userByRestId;

    private XGraphqlEndpoint userTweets;

    private XGraphqlEndpoint viewer;

    /**
     * @return 是否带齐 Bearer、User-Agent 和 {@link XGraphqlOperation} 的每个接口；插件新增接口后，之前缓存的文档不完整
     */
    @JsonIgnore
    public boolean isComplete() {
        return bearerToken != null && userAgent != null
                && userByScreenName != null && userByRestId != null && userTweets != null
                && viewer != null;
    }

    /**
     * @param operation GraphQL 接口
     * @return 该接口的当前参数
     */
    public XGraphqlEndpoint endpointOf(XGraphqlOperation operation) {
        XGraphqlEndpoint endpoint;
        switch (operation) {
            case USER_BY_SCREEN_NAME:
                endpoint = userByScreenName;
                break;
            case USER_BY_REST_ID:
                endpoint = userByRestId;
                break;
            case USER_TWEETS:
                endpoint = userTweets;
                break;
            case VIEWER:
                endpoint = viewer;
                break;
            default:
                throw new IllegalArgumentException("未知的 X GraphQL 接口: " + operation);
        }
        return Objects.requireNonNull(endpoint, "X 接口参数缺少 " + operation.getOperationName());
    }
}
