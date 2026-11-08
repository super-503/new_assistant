package com.zengame.ai.infrastructure.util;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zengame.ai.infrastructure.exception.JacksonProcessingException;

/**
 * @author john
 * @since 2022/10/11
 */
public class JacksonUtil {

    public static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();
    static {
        DEFAULT_MAPPER.registerModule(new JavaTimeModule());
        DEFAULT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static ObjectMapper objectMapper = DEFAULT_MAPPER;


    public static void setObjectMapper(ObjectMapper newInstance) {
        JacksonUtil.objectMapper = newInstance;
    }

    public static ObjectMapper getObjectMapper() {
        return JacksonUtil.objectMapper;
    }

    public static String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            String messageForDeveloper = StrUtil.format("Java对象Json序列化异常；Java类型：{}，ToString：{}。", value.getClass().getTypeName(), value.toString());
            throw new JacksonProcessingException(messageForDeveloper, e);
        }
    }

    public static JsonNode readTree(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (JsonProcessingException e) {
            String messageForDeveloper = StrUtil.format("反序列化文本为JsonNode异常；文本：{}，ToString：", content);
            throw new JacksonProcessingException(messageForDeveloper, e);
        }
    }

    public static <T> T readValue(String jsonText, Class<T> javaType) {
        try {
            return objectMapper.readValue(jsonText, javaType);
        } catch (JsonProcessingException e) {
            String messageForDeveloper = StrUtil.format("反序列化文本为Java对象异常；文本：{}，Java类型：{}", jsonText, javaType.getTypeName());
            throw new JacksonProcessingException(messageForDeveloper, e);
        }
    }

    public static <T> T readValue(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON解析失败", e);
        }
    }

}
