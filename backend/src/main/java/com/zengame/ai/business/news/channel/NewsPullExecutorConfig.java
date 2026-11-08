package com.zengame.ai.business.news.channel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 资讯手动拉取（「立即拉取」「重试」）的线程池。队列满时拒绝提交，由调用方提示运营稍后再试。
 */
@Configuration
public class NewsPullExecutorConfig {

    /**
     * @return 资讯手动拉取线程池
     */
    @Bean(name = "newsPullTaskExecutor")
    public ThreadPoolTaskExecutor newsPullTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("news-pull-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
