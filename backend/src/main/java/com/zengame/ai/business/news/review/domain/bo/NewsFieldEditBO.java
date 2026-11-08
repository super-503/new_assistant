package com.zengame.ai.business.news.review.domain.bo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 人工修改一个 AI 字段的值。
 */
@Data
public class NewsFieldEditBO {

    private String fieldKey;

    /**
     * 新值：文本、单选为字符串，数字为数值，多选为字符串数组；JSON null 表示清空
     */
    private JsonNode value;
}
