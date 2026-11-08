package com.zengame.ai.apiclient.x;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * X 插件用到的网页端 GraphQL 接口。接口地址、queryId 和 features 在运行时由 {@link XApiDocumentResolver} 按接口名读取。
 */
@Getter
@AllArgsConstructor
public enum XGraphqlOperation {

    /** 按 @用户名查用户 */
    USER_BY_SCREEN_NAME("UserByScreenName"),

    /** 按用户 rest_id 查用户 */
    USER_BY_REST_ID("UserByRestId"),

    /** 用户主页的推文时间线（含转推、自己的回复串） */
    USER_TWEETS("UserTweets"),

    /** 当前登录账号 */
    VIEWER("Viewer");

    /**
     * 接口名，即 API.json 里 graphql 下的键
     */
    private final String operationName;
}
