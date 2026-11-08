package com.zengame.ai.apiclient.x;

import com.zengame.ai.apiclient.x.vo.XApiDocument;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * X 网页端接口参数的取值入口：优先读 Redis 缓存，没有或不完整时由 {@link XApiDocumentLoader} 下载最新文档并缓存 6 小时。
 *
 * <p>同一 JVM 内并发请求缓存未命中时，只有一个线程下载，其余线程等它写入缓存后直接读取。
 */
@Component
@RequiredArgsConstructor
public class XApiDocumentResolver {

    private static final String CACHE_KEY = "news:x:api-document";

    private static final long CACHE_HOURS = 6;

    private final RedisUtil redisUtil;

    private final XApiDocumentLoader apiDocumentLoader;

    private final Object loadLock = new Object();

    /**
     * @return 当前的 X 网页端接口参数
     * @throws BusinessException 缓存未命中且文档获取或解析失败
     */
    public XApiDocument resolve() {
        return readCache().orElseGet(this::loadAndCache);
    }

    /**
     * 加锁后再查一次缓存，仍未命中才下载并写入缓存。
     */
    private XApiDocument loadAndCache() {
        synchronized (loadLock) {
            return readCache().orElseGet(() -> {
                XApiDocument document = apiDocumentLoader.load();
                redisUtil.setObject(CACHE_KEY, document, CACHE_HOURS, TimeUnit.HOURS);
                return document;
            });
        }
    }

    /**
     * 读缓存；缓存里的文档缺少插件当前用到的接口（插件新增接口之前写入的）时视为未命中。
     */
    private Optional<XApiDocument> readCache() {
        return Optional.ofNullable(redisUtil.getObject(CACHE_KEY, XApiDocument.class))
                .filter(XApiDocument::isComplete);
    }
}
