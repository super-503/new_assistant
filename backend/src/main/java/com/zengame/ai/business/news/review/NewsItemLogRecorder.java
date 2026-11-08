package com.zengame.ai.business.news.review;

import com.zengame.ai.business.news.domain.enums.NewsItemAction;
import com.zengame.ai.business.news.review.domain.bo.NewsItemLogDetailBO;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.mapper.NewsItemLogMapper;
import com.zengame.ai.repository.ai.po.NewsItemLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 追加审核与资讯库产生的资讯处理记录。
 */
@Component
@RequiredArgsConstructor
public class NewsItemLogRecorder {

    private final NewsItemLogMapper newsItemLogMapper;

    /**
     * 追加一条处理记录，发生时间取当前时间。
     *
     * @param itemId   资讯
     * @param action   动作
     * @param operator 操作人登录名
     * @param detail   附加信息；没有时为 null
     */
    public void record(Long itemId, NewsItemAction action, String operator, NewsItemLogDetailBO detail) {
        NewsItemLogPO log = new NewsItemLogPO();
        log.setItemId(itemId);
        log.setAction(action);
        log.setOperator(operator);
        log.setDetailJson(Optional.ofNullable(detail).map(JacksonUtil::writeValueAsString).orElse(null));
        newsItemLogMapper.insert(log);
    }
}
