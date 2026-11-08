package com.zengame.ai.business.news.channel.aibase;

import com.zengame.ai.apiclient.aibase.AiBaseClient;
import com.zengame.ai.apiclient.aibase.InformationVO;
import com.zengame.ai.business.news.channel.ChannelPlugin;
import com.zengame.ai.business.news.channel.bo.ChannelCredential;
import com.zengame.ai.business.news.channel.bo.ChannelDescriptor;
import com.zengame.ai.business.news.channel.bo.FeedCursor;
import com.zengame.ai.business.news.channel.bo.FetchResult;
import com.zengame.ai.business.news.channel.bo.FetchedItem;
import com.zengame.ai.business.news.channel.bo.SourceCandidate;
import com.zengame.ai.business.news.channel.bo.SourcePreview;
import com.zengame.ai.business.news.channel.bo.SourceRecentItem;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsCredentialMode;
import com.zengame.ai.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * AIBase 渠道插件：无需凭证、不支持搜索，固定一个订阅源「AIBase 资讯」，内容取自现有 AIBase 资讯接口的第一页。
 */
@Component
@RequiredArgsConstructor
public class AiBasePlugin implements ChannelPlugin {

    private static final String SOURCE_KEY = "news";

    private static final String SOURCE_NAME = "AIBase 资讯";

    private static final String ARTICLE_URL = "https://news.aibase.com/zh/news/%d";

    private static final int RECENT_ITEM_LIMIT = 10;

    private static final DateTimeFormatter ADD_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 图片 CDN 拒绝不带浏览器 User-Agent 的请求
     */
    private static final ChannelDescriptor DESCRIPTOR = ChannelDescriptor.builder()
            .code("aibase")
            .name("AIBase")
            .domain("news.aibase.com")
            .defaultContentType(NewsContentType.ARTICLE)
            .credentialMode(NewsCredentialMode.NONE)
            .searchable(false)
            .embedDomains(Collections.emptyList())
            .build();

    private final AiBaseClient aiBaseClient;

    @Override
    public ChannelDescriptor descriptor() {
        return DESCRIPTOR;
    }

    /**
     * 不支持搜索，忽略关键词，返回唯一的固定来源「AIBase 资讯」。
     */
    @Override
    public List<SourceCandidate> search(String keyword, ChannelCredential credential) {
        return Collections.singletonList(source());
    }

    /**
     * @throws BusinessException 来源标识不是固定来源
     */
    @Override
    public SourcePreview preview(String sourceKey, ChannelCredential credential) {
        checkSourceKey(sourceKey);
        List<SourceRecentItem> recentItems = queryFirstPage().stream()
                .limit(RECENT_ITEM_LIMIT)
                .map(this::toRecentItem)
                .collect(Collectors.toList());
        return SourcePreview.builder()
                .source(source())
                .contentType(NewsContentType.ARTICLE)
                .recentItems(recentItems)
                .build();
    }

    /**
     * 取资讯接口第一页中添加时间晚于拉取进度的资讯。
     *
     * @throws BusinessException 来源标识不是固定来源，或资讯接口请求失败
     */
    @Override
    public FetchResult fetch(FeedCursor cursor, ChannelCredential credential) {
        checkSourceKey(cursor.getSourceKey());
        List<FetchedItem> items = queryFirstPage().stream()
                .map(this::toFetchedItem)
                .filter(item -> item.getFeedTime().isAfter(cursor.getSince()))
                .collect(Collectors.toList());
        return FetchResult.builder().items(items).build();
    }

    private List<InformationVO> queryFirstPage() {
        return Objects.requireNonNull(aiBaseClient.queryInformationData(1), "AIBase 资讯接口返回为空");
    }

    private SourceCandidate source() {
        return SourceCandidate.builder()
                .sourceKey(SOURCE_KEY)
                .name(SOURCE_NAME)
                .stats(Collections.emptyList())
                .build();
    }

    private FetchedItem toFetchedItem(InformationVO information) {
        LocalDateTime addTime = parseAddTime(information);
        return FetchedItem.builder()
                .canonicalUrl(String.format(ARTICLE_URL, information.getId()))
                .title(information.getTitle())
                .bodyHtml(StringUtils.defaultIfBlank(information.getSummary(), null))
                .coverUrl(StringUtils.defaultIfBlank(information.getThumb(), null))
                .author(StringUtils.defaultIfBlank(information.getSourcename(), null))
                .publishTime(addTime)
                .feedTime(addTime)
                .stats(Collections.emptyList())
                .build();
    }

    private SourceRecentItem toRecentItem(InformationVO information) {
        return SourceRecentItem.builder()
                .title(information.getTitle())
                .thumbnailUrl(StringUtils.defaultIfBlank(information.getThumb(), null))
                .publishTime(parseAddTime(information))
                .stats(Collections.emptyList())
                .build();
    }

    private LocalDateTime parseAddTime(InformationVO information) {
        return LocalDateTime.parse(information.getAddtime(), ADD_TIME_FORMATTER);
    }

    private void checkSourceKey(String sourceKey) {
        if (!SOURCE_KEY.equals(sourceKey)) {
            throw new BusinessException("AIBase 没有该来源: " + sourceKey);
        }
    }
}
