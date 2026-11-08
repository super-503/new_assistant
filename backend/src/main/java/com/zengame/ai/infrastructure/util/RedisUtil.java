package com.zengame.ai.infrastructure.util;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** 资讯采集锁、扫码会话与渠道接口缓存。 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate stringRedisTemplate;

    public boolean setIfAbsent(String cacheKey, String value, long timeout, TimeUnit unit) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(cacheKey, value, timeout, unit);
        return Boolean.TRUE.equals(success);
    }

    public void setObject(String cacheKey, Object value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(cacheKey, JacksonUtil.writeValueAsString(value), timeout, unit);
    }

    public <T> T getObject(String cacheKey, Class<T> type) {
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        return StringUtils.isBlank(json) ? null : JacksonUtil.readValue(json, type);
    }

    public boolean delete(String cacheKey) {
        return stringRedisTemplate.delete(cacheKey);
    }

    public boolean hasKey(String cacheKey) {
        return stringRedisTemplate.hasKey(cacheKey);
    }
}
