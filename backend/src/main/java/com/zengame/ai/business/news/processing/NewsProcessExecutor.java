package com.zengame.ai.business.news.processing;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 数据加工的模型调用线程池：线程内切换为计费用户的登录上下文，调用方线程的上下文不受影响。
 *
 * <p>队列满时拒绝提交，不在调用方线程执行。
 */
@Component
public class NewsProcessExecutor extends ThreadPoolTaskExecutor {

    /**
     * 同时进行的模型调用数
     */
    static final int POOL_SIZE = 4;

    private static final int QUEUE_CAPACITY = 100;

    public NewsProcessExecutor() {
        setCorePoolSize(POOL_SIZE);
        setMaxPoolSize(POOL_SIZE);
        setQueueCapacity(QUEUE_CAPACITY);
        setThreadNamePrefix("news-process-");
        setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    }
}
