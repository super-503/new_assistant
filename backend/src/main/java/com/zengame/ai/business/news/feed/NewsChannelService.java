package com.zengame.ai.business.news.feed;

import com.zengame.ai.business.news.channel.ChannelPluginRegistry;
import com.zengame.ai.business.news.channel.bo.ChannelDescriptor;
import com.zengame.ai.business.news.domain.enums.NewsCredentialStatus;
import com.zengame.ai.business.news.feed.cover.NewsChannelConvert;
import com.zengame.ai.business.news.feed.domain.bo.FeedStateBO;
import com.zengame.ai.business.news.feed.domain.enums.NewsFeedStatus;
import com.zengame.ai.business.news.feed.domain.vo.ChannelRunVO;
import com.zengame.ai.business.news.feed.domain.vo.ChannelTodoVO;
import com.zengame.ai.business.news.feed.domain.vo.ChannelVO;
import com.zengame.ai.business.news.feed.domain.vo.FeedVO;
import com.zengame.ai.business.news.feed.domain.vo.TodoFeedVO;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.NewsChannelMapper;
import com.zengame.ai.repository.ai.mapper.NewsChannelRunMapper;
import com.zengame.ai.repository.ai.mapper.NewsFeedMapper;
import com.zengame.ai.repository.ai.po.NewsChannelPO;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 渠道管理：渠道列表、渠道设置、运行记录、「需处理」汇总。
 */
@Service
@RequiredArgsConstructor
public class NewsChannelService {

    /**
     * 运行记录侧边栏展示的最近条数
     */
    private static final int RECENT_RUN_LIMIT = 50;

    private final NewsChannelMapper newsChannelMapper;

    private final NewsFeedMapper newsFeedMapper;

    private final NewsChannelRunMapper newsChannelRunMapper;

    private final ChannelPluginRegistry channelPluginRegistry;

    private final NewsFeedService newsFeedService;

    /**
     * @return 全部渠道，按渠道主键升序，带凭证状态、订阅源数量和是否需要处理
     */
    public List<ChannelVO> listChannels() {
        return newsChannelMapper.selectAll().stream()
                .map(this::toChannelVO)
                .collect(Collectors.toList());
    }

    /**
     * @param channelCode 渠道标识
     * @return 渠道视图
     * @throws BusinessException 渠道不存在
     */
    public ChannelVO getChannel(String channelCode) {
        return toChannelVO(requireChannel(channelCode));
    }

    /**
     * 保存渠道设置：启用或停用渠道。
     *
     * @param channelCode 渠道标识
     * @param enabled     是否启用
     * @throws BusinessException 渠道不存在
     */
    public void updateChannelEnabled(String channelCode, Boolean enabled) {
        if (newsChannelMapper.updateEnabledByCode(channelCode, enabled) != 1) {
            throw new BusinessException("资讯渠道不存在: " + channelCode);
        }
    }

    /**
     * 渠道最近 50 次运行，按开始时间倒序；手动触发的带订阅源显示名称。
     *
     * @param channelCode 渠道标识
     * @return 运行记录
     * @throws BusinessException 渠道不存在
     */
    public List<ChannelRunVO> listRuns(String channelCode) {
        requireChannel(channelCode);
        Map<Long, String> feedNames = newsFeedMapper.selectByChannelCode(channelCode).stream()
                .collect(Collectors.toMap(NewsFeedPO::getId, NewsFeedPO::getDisplayName));
        return newsChannelRunMapper.selectRecentByChannelCode(channelCode, RECENT_RUN_LIMIT).stream()
                .map(run -> NewsChannelConvert.INSTANCE.toRunVO(run,
                        Optional.ofNullable(run.getFeedId()).map(feedNames::get).orElse(null)))
                .collect(Collectors.toList());
    }

    /**
     * 「需处理」汇总：凭证已失效的渠道；其余渠道下拉取失败的订阅源（带原因）。
     * 凭证已失效的渠道下的订阅源只算渠道那一项，不重复列出。
     *
     * @return 需处理事项
     */
    public ChannelTodoVO todo() {
        List<ChannelVO> credentialChannels = new ArrayList<>();
        List<TodoFeedVO> failedFeeds = new ArrayList<>();
        for (NewsChannelPO channel : newsChannelMapper.selectAll()) {
            ChannelVO channelVO = toChannelVO(channel);
            if (channel.getCredentialStatus() == NewsCredentialStatus.INVALID) {
                credentialChannels.add(channelVO);
                continue;
            }
            for (FeedVO feed : newsFeedService.listFeeds(channel.getCode(), NewsFeedStatus.ERROR.getCode(), null)) {
                failedFeeds.add(NewsChannelConvert.INSTANCE.toTodoFeedVO(channelVO, feed));
            }
        }
        return NewsChannelConvert.INSTANCE.toTodoVO(credentialChannels, failedFeeds);
    }

    /**
     * 渠道记录转渠道视图：能力字段取自插件描述；凭证已失效或有订阅源拉取失败时需要处理。
     */
    private ChannelVO toChannelVO(NewsChannelPO channel) {
        ChannelDescriptor descriptor = channelPluginRegistry.getPlugin(channel.getCode()).descriptor();
        List<NewsFeedPO> feeds = newsFeedMapper.selectByChannelCode(channel.getCode());
        boolean needsAttention = channel.getCredentialStatus() == NewsCredentialStatus.INVALID
                || feeds.stream().anyMatch(feed -> FeedStateBO.resolve(feed, channel.getCredentialStatus()).isFailed());
        return NewsChannelConvert.INSTANCE.toVO(channel, descriptor, feeds.size(), needsAttention);
    }

    private NewsChannelPO requireChannel(String channelCode) {
        return Optional.ofNullable(newsChannelMapper.selectByCode(channelCode))
                .orElseThrow(() -> new BusinessException("资讯渠道不存在: " + channelCode));
    }
}
