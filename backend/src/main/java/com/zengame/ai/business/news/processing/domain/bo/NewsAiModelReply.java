package com.zengame.ai.business.news.processing.domain.bo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 一次数据加工模型调用的返回。
 */
@Getter
@AllArgsConstructor
public class NewsAiModelReply {

    /**
     * 任务记录的链路ID，任务记录归属计费用户
     */
    private final String traceId;

    /**
     * 模型输出的正文
     */
    private final String content;
}
