package com.zengame.ai.business.news.channel;

import com.zengame.ai.business.news.channel.bo.FetchedItem;
import com.zengame.ai.business.news.channel.cover.NewsItemIngestConvert;
import com.zengame.ai.business.news.domain.enums.NewsItemAction;
import com.zengame.ai.repository.ai.mapper.NewsItemLogMapper;
import com.zengame.ai.repository.ai.mapper.NewsItemMapper;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import com.zengame.ai.repository.ai.po.NewsItemLogPO;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

/**
 * 资讯入库：统一条目按原文链接去重，保留源媒体地址，正文清洗后写入 news_item（加工中）和一条 COLLECTED 处理记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemIngestService {

    private final ContentSanitizer contentSanitizer;

    private final NewsItemMapper newsItemMapper;

    private final NewsItemLogMapper newsItemLogMapper;

    private final TransactionTemplate transactionTemplate;

    /**
     * 入库一条统一条目。
     *
     * @param plugin 所属渠道插件，提供内嵌播放器域名和媒体下载
     * @param feed   来源订阅源，资讯沿用它的内容类型
     * @param item   插件输出的统一条目
     * @return 新资讯主键；原文链接已在库里时为空
     * @throws RuntimeException 入库失败，由拉取流程记为该订阅源拉取失败
     */
    public Optional<Long> ingest(ChannelPlugin plugin, NewsFeedPO feed, FetchedItem item) {
        if (newsItemMapper.selectByCanonicalUrl(item.getCanonicalUrl()) != null) {
            return Optional.empty();
        }
        String bodyHtml = Optional.ofNullable(item.getBodyHtml())
                .filter(StringUtils::isNotBlank)
                .map(html -> contentSanitizer.sanitize(html, plugin.descriptor().getEmbedDomains()))
                .filter(StringUtils::isNotBlank)
                .orElse(null);
        NewsItemPO itemPO = NewsItemIngestConvert.INSTANCE.toItemPO(feed, item, bodyHtml);
        transactionTemplate.executeWithoutResult(status -> {
            newsItemMapper.insert(itemPO);
            newsItemLogMapper.insert(collectedLog(itemPO.getId()));
        });
        log.info("资讯入库, itemId={}, feedId={}, url={}", itemPO.getId(), feed.getId(), item.getCanonicalUrl());
        return Optional.of(itemPO.getId());
    }

    private NewsItemLogPO collectedLog(Long itemId) {
        NewsItemLogPO logPO = new NewsItemLogPO();
        logPO.setItemId(itemId);
        logPO.setAction(NewsItemAction.COLLECTED);
        return logPO;
    }
}
