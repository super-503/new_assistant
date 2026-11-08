package com.zengame.ai.apiclient.x.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * X 网页端一个 GraphQL 接口的当前参数，取自 TwitterInternalAPIDocument。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XGraphqlEndpoint {

    /**
     * 接口地址，如 https://x.com/i/api/graphql/{queryId}/UserTweets
     */
    private String url;

    /**
     * 接口标识
     */
    private String queryId;

    /**
     * 功能开关参数，原样作为请求参数 features 的 JSON 对象
     */
    private JsonNode features;
}
