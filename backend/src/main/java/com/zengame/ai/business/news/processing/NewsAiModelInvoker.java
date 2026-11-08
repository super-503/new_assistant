package com.zengame.ai.business.news.processing;

import com.zengame.ai.apiclient.model.NewsTextClient;
import com.zengame.ai.business.news.processing.domain.bo.NewsAiModelReply;
import com.zengame.ai.business.news.setting.domain.bo.NewsAiModelConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/** 将资讯的模型加工请求提交到专用线程池。 */
@Component
@RequiredArgsConstructor
public class NewsAiModelInvoker {

    private final NewsTextClient newsTextClient;
    private final NewsProcessExecutor newsProcessExecutor;

    /**
     * @param config 本次加工使用的模型配置
     * @param prompt 资讯加工提示词
     * @return 模型输出，调用失败时异常完成
     */
    public CompletableFuture<NewsAiModelReply> submit(NewsAiModelConfig config, String prompt) {
        return CompletableFuture.supplyAsync(() -> newsTextClient.complete(config, prompt), newsProcessExecutor);
    }
}
