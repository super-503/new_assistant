package com.zengame.ai.business.news.processing.domain.bo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 一次生成按字段校验后的结果：合法的值和未生成的字段。
 */
@Getter
@AllArgsConstructor
public class NewsAiGeneration {

    /**
     * 校验通过的字段值：{字段标识: 值}，值的格式按输出类型
     */
    private final ObjectNode values;

    /**
     * 未生成的字段标识：缺失、类型不符或取值在选项外
     */
    private final List<String> failedKeys;
}
