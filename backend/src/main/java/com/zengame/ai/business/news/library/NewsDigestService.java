package com.zengame.ai.business.news.library;

import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import com.zengame.ai.repository.ai.mapper.NewsItemMapper;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** 推送已审核资讯，按资讯记录日报发送结果。 */
@Service
@RequiredArgsConstructor
public class NewsDigestService {

    private static final int MAX_BATCH_SIZE = 20;

    // 为飞书 20 KB 请求体上限预留 JSON 转义和消息结构的空间。
    private static final int MAX_MESSAGE_LENGTH = 3000;

    private final NewsItemMapper newsItemMapper;

    /**
     * 推送一批仍为已通过且尚未发送的资讯。飞书确认接收后，记录本批资讯的发送时间。
     *
     * @param sender 本次日报使用的通知发送器
     * @return 本批已发送条数；没有待发资讯时为零
     * @throws RuntimeException 推送或发送记录写入失败，待发资讯留在后续任务中重试
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public int sendBatch(Consumer<String> sender) {
        List<NewsItemPO> items = newsItemMapper.selectUnsentDigestItemsForUpdate(NewsItemStatus.APPROVED, MAX_BATCH_SIZE);
        if (CollectionUtils.isEmpty(items)) {
            return 0;
        }

        StringBuilder message = new StringBuilder("资讯日报");
        List<Long> sentIds = new ArrayList<>();
        for (NewsItemPO item : items) {
            String entry = formatEntry(item);
            if (message.length() + entry.length() > MAX_MESSAGE_LENGTH) {
                break;
            }
            message.append(entry);
            sentIds.add(item.getId());
        }
        sender.accept(message.toString());
        newsItemMapper.markDigestSent(sentIds);
        return sentIds.size();
    }

    /**
     * @param item 已通过资讯
     * @return 日报中的原标题、可用的作者名和原文链接
     */
    private String formatEntry(NewsItemPO item) {
        String title = Optional.ofNullable(item.getOriginTitle())
                .filter(StringUtils::isNotBlank)
                .map(this::plainText)
                .orElse("查看原文");
        String author = Optional.ofNullable(item.getAuthor())
                .filter(StringUtils::isNotBlank)
                .map(name -> " · " + plainText(name))
                .orElse("");
        return "\n\n• " + title + author + "\n" + item.getCanonicalUrl();
    }

    /**
     * @param text 来源提供的标题或作者名
     * @return 日报中单行展示的文本
     */
    private String plainText(String text) {
        return StringUtils.normalizeSpace(text).replace('<', '＜');
    }
}
