package com.zengame.ai.business.news.processing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zengame.ai.infrastructure.util.JacksonUtil;

import java.util.Collection;
import java.util.List;

/**
 * 资讯模块里 JSON 字符串数组列的读写：AI 字段的选项、资讯的未生成字段标识和已修改字段标识。
 */
public final class StringListJson {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private StringListJson() {
    }

    /**
     * @param json JSON 字符串数组
     * @return 数组元素，保持原顺序
     */
    public static List<String> read(String json) {
        return JacksonUtil.readValue(json, STRING_LIST);
    }

    /**
     * @param values 字符串集合
     * @return JSON 字符串数组，元素按集合的遍历顺序
     */
    public static String write(Collection<String> values) {
        return JacksonUtil.writeValueAsString(values);
    }
}
