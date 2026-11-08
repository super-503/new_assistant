package com.zengame.ai.business.news.processing.domain.bo;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 一条资讯一次加工的结局：字段结果，以及模型任务的链路ID和整次调用失败的原因。
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NewsAiOutcome {

    @Getter
    private final NewsAiGeneration generation;

    private final String traceId;

    private final String error;

    /**
     * 没有参与生成的字段，不调用模型。
     *
     * @return 空结果
     */
    public static NewsAiOutcome nothingToGenerate() {
        return new NewsAiOutcome(new NewsAiGeneration(JsonNodeFactory.instance.objectNode(), Collections.emptyList()),
                null, null);
    }

    /**
     * 模型正常返回，字段已逐个校验。
     *
     * @param generation 校验后的字段结果
     * @param traceId    模型任务链路ID
     * @return 生成结局
     */
    public static NewsAiOutcome generated(NewsAiGeneration generation, String traceId) {
        return new NewsAiOutcome(generation, traceId, null);
    }

    /**
     * 整次调用失败，参与生成的字段全部记为未生成。
     *
     * @param fieldKeys 参与生成的字段标识
     * @param traceId   模型任务链路ID；调用未产生任务记录时为空
     * @param error     失败原因
     * @return 失败结局
     */
    public static NewsAiOutcome failed(List<String> fieldKeys, String traceId, String error) {
        return new NewsAiOutcome(new NewsAiGeneration(JsonNodeFactory.instance.objectNode(), fieldKeys),
                traceId, error);
    }

    /**
     * @return 模型任务链路ID；没有调用模型或调用未产生任务记录时为空
     */
    public Optional<String> getTraceId() {
        return Optional.ofNullable(traceId);
    }

    /**
     * @return 整次调用失败的原因；调用正常时为空
     */
    public Optional<String> getError() {
        return Optional.ofNullable(error);
    }
}
