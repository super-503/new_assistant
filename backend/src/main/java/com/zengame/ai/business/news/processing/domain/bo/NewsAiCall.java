package com.zengame.ai.business.news.processing.domain.bo;

import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 一条资讯已发起的加工：参与生成的字段和进行中的模型调用。
 */
@AllArgsConstructor
public class NewsAiCall {

    @Getter
    private final NewsItemPO item;

    /**
     * 参与生成的字段，按展示顺序
     */
    @Getter
    private final List<NewsAiFieldPO> fields;

    private final CompletableFuture<NewsAiModelReply> reply;

    /**
     * @return 进行中的模型调用；没有参与生成的字段时为空
     */
    public Optional<CompletableFuture<NewsAiModelReply>> getReply() {
        return Optional.ofNullable(reply);
    }
}
