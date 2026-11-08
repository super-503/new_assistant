package com.zengame.ai.business.news.review.domain.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 一个 AI 字段的新值。
 */
@Data
public class NewsFieldValueRequest {

    @NotBlank
    private String fieldKey;

    /**
     * 新值：文本、单选为字符串，数字为数值，多选为字符串数组；null 表示清空
     */
    @NotNull
    private JsonNode value;
}
