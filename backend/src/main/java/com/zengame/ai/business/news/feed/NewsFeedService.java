package com.zengame.ai.business.news.feed;

import com.zengame.ai.business.news.channel.ChannelCredentialService;
import com.zengame.ai.business.news.channel.ChannelLoginExpiredException;
import com.zengame.ai.business.news.channel.ChannelPlugin;
import com.zengame.ai.business.news.channel.ChannelPluginRegistry;
import com.zengame.ai.business.news.channel.ChannelPullService;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.feed.cover.NewsFeedConvert;
import com.zengame.ai.business.news.feed.domain.bo.FeedStateBO;
import com.zengame.ai.business.news.feed.domain.enums.NewsFeedStatus;
import com.zengame.ai.business.news.feed.domain.request.FeedCreateRequest;
import com.zengame.ai.business.news.feed.domain.request.FeedUpdateRequest;
import com.zengame.ai.business.news.feed.domain.vo.FeedVO;
import com.zengame.ai.business.news.feed.domain.vo.SourceCandidateVO;
import com.zengame.ai.business.news.feed.domain.vo.SourcePreviewVO;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import com.zengame.ai.repository.ai.mapper.NewsChannelMapper;
import com.zengame.ai.repository.ai.mapper.NewsFeedMapper;
import com.zengame.ai.repository.ai.po.NewsChannelPO;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 订阅源管理：列表、订阅、编辑、启停、删除、搜索来源、来源预览、立即拉取和重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsFeedService {

    private final NewsChannelMapper newsChannelMapper;

    private final NewsFeedMapper newsFeedMapper;

    private final ChannelPluginRegistry channelPluginRegistry;

    private final ChannelCredentialService channelCredentialService;

    private final ChannelPullService channelPullService;

    /**
     * 渠道下的订阅源，按主键升序。状态按展示规则推导：已停用优先；渠道凭证失效时启用中的订阅源为拉取失败。
     *
     * @param channelCode 渠道标识
     * @param status      状态筛选编码（OK / ERROR / PENDING / DISABLED）；为空不筛选
     * @param keyword     按显示名称模糊筛选，不区分大小写；为空不筛选
     * @return 订阅源表格行
     * @throws BusinessException 渠道不存在或状态编码无效
     */
    public List<FeedVO> listFeeds(String channelCode, String status, String keyword) {
        NewsChannelPO channel = requireChannel(channelCode);
        Optional<NewsFeedStatus> statusFilter = Optional.ofNullable(StringUtils.trimToNull(status))
                .map(code -> CodeEnumUtils.fromCode(code, NewsFeedStatus.class));
        Optional<String> keywordFilter = Optional.ofNullable(StringUtils.trimToNull(keyword));
        List<FeedVO> rows = new ArrayList<>();
        for (NewsFeedPO feed : newsFeedMapper.selectByChannelCode(channelCode)) {
            FeedStateBO state = FeedStateBO.resolve(feed, channel.getCredentialStatus());
            boolean statusMatched = statusFilter.map(expected -> expected == state.getStatus()).orElse(true);
            boolean keywordMatched = keywordFilter
                    .map(word -> StringUtils.containsIgnoreCase(feed.getDisplayName(), word))
                    .orElse(true);
            if (statusMatched && keywordMatched) {
                rows.add(NewsFeedConvert.INSTANCE.toVO(feed, state, channelPullService.isPulling(feed.getId())));
            }
        }
        return rows;
    }

    /**
     * 订阅一个从搜索结果里选中的来源，状态为待首次拉取。
     *
     * @param channelCode 渠道标识
     * @param request     来源信息、显示名称、内容类型、是否启用
     * @throws BusinessException 渠道不存在、内容类型无效，或同一渠道已订阅该来源（「该来源已订阅」）
     */
    public void createFeed(String channelCode, FeedCreateRequest request) {
        requireChannel(channelCode);
        if (newsFeedMapper.selectByChannelAndSourceKey(channelCode, request.getSourceKey()) != null) {
            throw new BusinessException("该来源已订阅");
        }
        NewsFeedPO feed = NewsFeedConvert.INSTANCE.toPO(channelCode, request);
        newsFeedMapper.insert(feed);
        log.info("资讯订阅源已添加, channel={}, feedId={}, sourceKey={}", channelCode, feed.getId(), feed.getSourceKey());
    }

    /**
     * 编辑订阅源的显示名称、内容类型、是否启用。
     *
     * @param feedId  订阅源 id
     * @param request 新的设置
     * @throws BusinessException 订阅源不存在或内容类型无效
     */
    public void updateFeed(Long feedId, FeedUpdateRequest request) {
        NewsContentType contentType = CodeEnumUtils.fromCode(request.getContentType(), NewsContentType.class);
        int updated = newsFeedMapper.updateSettingsById(feedId, request.getDisplayName(), contentType, request.getEnabled());
        requireUpdated(updated, feedId);
    }

    /**
     * 订阅源行内的启用开关。
     *
     * @param feedId  订阅源 id
     * @param enabled 是否启用
     * @throws BusinessException 订阅源不存在
     */
    public void updateFeedEnabled(Long feedId, Boolean enabled) {
        requireUpdated(newsFeedMapper.updateEnabledById(feedId, enabled), feedId);
    }

    /**
     * 物理删除订阅源，已入库的资讯保留。
     *
     * @param feedId 订阅源 id
     * @throws BusinessException 订阅源不存在
     */
    public void deleteFeed(Long feedId) {
        requireUpdated(newsFeedMapper.deleteById(feedId), feedId);
        log.info("资讯订阅源已删除, feedId={}", feedId);
    }

    /**
     * 在渠道内搜索来源；不支持搜索的渠道忽略关键词，返回唯一的来源。
     *
     * @param channelCode 渠道标识
     * @param keyword     关键词；支持搜索的渠道必填
     * @return 搜索结果，搜不到时为空列表
     * @throws BusinessException            渠道不存在，或支持搜索的渠道未填关键词
     * @throws ChannelLoginExpiredException 渠道凭证不可用，原因直接提示给运营
     */
    public List<SourceCandidateVO> searchSources(String channelCode, String keyword) {
        ChannelPlugin plugin = channelPluginRegistry.getPlugin(channelCode);
        String trimmed = StringUtils.trimToNull(keyword);
        if (plugin.descriptor().isSearchable() && trimmed == null) {
            throw new BusinessException("请输入搜索关键词");
        }
        return NewsFeedConvert.INSTANCE.toCandidateVOs(
                plugin.search(trimmed, channelCredentialService.loadCredential(channelCode)));
    }

    /**
     * 预览一个来源：来源信息、内容类型和最近更新。
     *
     * @param channelCode 渠道标识
     * @param sourceKey   来源标识
     * @return 来源卡片
     * @throws BusinessException            渠道不存在
     * @throws ChannelLoginExpiredException 渠道凭证不可用，原因直接提示给运营
     */
    public SourcePreviewVO previewSource(String channelCode, String sourceKey) {
        ChannelPlugin plugin = channelPluginRegistry.getPlugin(channelCode);
        return NewsFeedConvert.INSTANCE.toPreviewVO(
                plugin.preview(sourceKey, channelCredentialService.loadCredential(channelCode)));
    }

    /**
     * 立即拉取或重试一个订阅源，提交后异步执行。
     *
     * @param feedId 订阅源 id
     * @throws BusinessException 订阅源不存在、正在拉取，或渠道凭证失效（「重试失败：渠道凭证失效」）
     */
    public void pullFeed(Long feedId) {
        channelPullService.pullFeedAsync(feedId);
    }

    private NewsChannelPO requireChannel(String channelCode) {
        return Optional.ofNullable(newsChannelMapper.selectByCode(channelCode))
                .orElseThrow(() -> new BusinessException("资讯渠道不存在: " + channelCode));
    }

    private static void requireUpdated(int updated, Long feedId) {
        if (updated != 1) {
            throw new BusinessException("订阅源不存在: " + feedId);
        }
    }
}
