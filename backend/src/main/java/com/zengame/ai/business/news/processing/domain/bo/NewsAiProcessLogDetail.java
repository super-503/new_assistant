package com.zengame.ai.business.news.processing.domain.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * PROCESSED / PROCESS_FAILED 处理记录的附加信息，序列化后存入 news_item_log.detail_json。
 */
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewsAiProcessLogDetail {

    /**
     * 本次参与生成的字段标识
     */
    private final List<String> fieldKeys;

    /**
     * 本次未生成的字段标识
     */
    private final List<String> failedKeys;

    /**
     * 模型任务链路ID；没有调用模型或调用未产生任务记录时不输出
     */
    private final String traceId;

    /**
     * 整次调用失败的原因；调用正常时不输出
     */
    private final String error;
}
