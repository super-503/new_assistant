package com.zengame.ai.business.news.channel;

import com.zengame.ai.business.news.channel.bo.ChannelCredential;
import com.zengame.ai.business.news.channel.bo.FeedCursor;
import com.zengame.ai.business.news.channel.bo.FetchedItem;
import com.zengame.ai.business.news.domain.enums.NewsCredentialStatus;
import com.zengame.ai.business.news.domain.enums.NewsFeedPullStatus;
import com.zengame.ai.business.news.domain.enums.NewsRunStatus;
import com.zengame.ai.business.news.domain.enums.NewsRunTriggerType;
import com.zengame.ai.business.news.processing.JobFamilySyncService;
import com.zengame.ai.business.news.processing.NewsAiProcessService;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.RedisUtil;
import com.zengame.ai.repository.ai.mapper.NewsChannelMapper;
import com.zengame.ai.repository.ai.mapper.NewsChannelRunMapper;
import com.zengame.ai.repository.ai.mapper.NewsFeedMapper;
import com.zengame.ai.repository.ai.po.NewsChannelPO;
import com.zengame.ai.repository.ai.po.NewsChannelRunPO;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 资讯拉取编排：定时任务拉取全部启用中的渠道，「立即拉取」「重试」异步拉取单个订阅源。
 *
 * <p>每个渠道先续期凭证，再依次拉取订阅源并入库；单个订阅源出错记为拉取失败并继续，登录失效则把凭证置为已失效并中止该渠道。
 * 每个渠道每次运行写一条运行记录，新入库的资讯交给数据加工。订阅源拉取期间持有 Redis 锁
 * {@code news:pull:feed:{feedId}}，前端据此显示「拉取中」并禁止重复触发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelPullService {

    private static final String FEED_LOCK_KEY = "news:pull:feed:%d";

    private static final String FEED_LOCK_VALUE = "1";

    private static final long FEED_LOCK_MINUTES = 30;

    private static final long FIRST_PULL_DAYS = 7;

    private static final int PULL_ERROR_MAX_LENGTH = 1000;

    private final ChannelPluginRegistry channelPluginRegistry;

    private final ChannelCredentialService channelCredentialService;

    private final ItemIngestService itemIngestService;

    private final NewsAiProcessService newsAiProcessService;

    private final JobFamilySyncService jobFamilySyncService;

    private final NewsChannelMapper newsChannelMapper;

    private final NewsFeedMapper newsFeedMapper;

    private final NewsChannelRunMapper newsChannelRunMapper;

    private final RedisUtil redisUtil;

    private final ThreadPoolTaskExecutor newsPullTaskExecutor;

    /**
     * 定时拉取：同步岗位分类选项，依次拉取每个启用中的渠道，新资讯同步交给数据加工。
     * 凭证已失效的渠道跳过并记为失败；订阅源正在手动拉取时本次跳过该订阅源。
     *
     * @throws BusinessException 本轮有渠道失败，xxl-job 据此把这次执行标记为失败
     */
    public void pullAll() {
        jobFamilySyncService.sync();
        List<Long> newItemIds = new ArrayList<>();
        List<String> failedChannels = new ArrayList<>();
        newsChannelMapper.selectAll().stream()
                .filter(NewsChannelPO::getEnabled)
                .forEach(channel -> {
                    ChannelRunOutcome outcome = runChannel(channel, NewsRunTriggerType.SCHEDULE, null,
                            () -> newsFeedMapper.selectEnabledByChannelCode(channel.getCode()));
                    newItemIds.addAll(outcome.getNewItemIds());
                    if (outcome.isFailed()) {
                        failedChannels.add(channel.getName());
                    }
                });
        processNewItems(newItemIds);
        if (!failedChannels.isEmpty()) {
            throw new BusinessException("资讯拉取失败的渠道：" + String.join("、", failedChannels));
        }
    }

    /**
     * 立即拉取或重试单个订阅源：加锁后提交线程池异步执行，运行记录的触发方式为 MANUAL。
     *
     * @param feedId 订阅源主键
     * @throws BusinessException 订阅源不存在、渠道凭证已失效、订阅源正在拉取中或线程池已满
     */
    public void pullFeedAsync(Long feedId) {
        NewsFeedPO feed = selectFeed(feedId);
        NewsChannelPO channel = newsChannelMapper.selectByCode(feed.getChannelCode());
        if (channel.getCredentialStatus() == NewsCredentialStatus.INVALID) {
            throw new BusinessException("重试失败：渠道凭证失效");
        }
        String lockKey = feedLockKey(feedId);
        if (!redisUtil.setIfAbsent(lockKey, FEED_LOCK_VALUE, FEED_LOCK_MINUTES, TimeUnit.MINUTES)) {
            throw new BusinessException("订阅源正在拉取中，请稍后再试");
        }
        try {
            newsPullTaskExecutor.execute(() -> pullFeedManually(feedId, lockKey));
        } catch (TaskRejectedException e) {
            redisUtil.delete(lockKey);
            log.warn("资讯手动拉取任务提交被拒绝, feedId={}", feedId, e);
            throw new BusinessException("拉取任务过多，请稍后再试");
        }
    }

    /**
     * @param feedId 订阅源主键
     * @return 订阅源是否正在拉取
     */
    public boolean isPulling(Long feedId) {
        return redisUtil.hasKey(feedLockKey(feedId));
    }

    /**
     * 线程池内执行的手动拉取：重新读取订阅源和渠道，拉取后把新资讯交给数据加工，结束时释放订阅源锁。
     */
    private void pullFeedManually(Long feedId, String lockKey) {
        try {
            NewsFeedPO feed = selectFeed(feedId);
            NewsChannelPO channel = newsChannelMapper.selectByCode(feed.getChannelCode());
            ChannelRunOutcome outcome = runChannel(channel, NewsRunTriggerType.MANUAL, feedId,
                    () -> Collections.singletonList(feed));
            processNewItems(outcome.getNewItemIds());
        } catch (RuntimeException e) {
            log.error("资讯手动拉取失败, feedId={}", feedId, e);
            throw e;
        } finally {
            redisUtil.delete(lockKey);
        }
    }

    /**
     * 运行一个渠道：续期凭证、依次拉取订阅源，写一条运行记录。定时运行逐个订阅源加锁，手动运行的锁由调用方持有。
     *
     * @param channel     渠道
     * @param triggerType 触发方式
     * @param runFeedId   手动运行拉取的订阅源；定时运行为空
     * @param feeds       本次要拉取的订阅源
     * @return 新入库的资讯主键和本次运行是否失败
     */
    private ChannelRunOutcome runChannel(NewsChannelPO channel, NewsRunTriggerType triggerType, Long runFeedId,
                                         Supplier<List<NewsFeedPO>> feeds) {
        LocalDateTime startTime = LocalDateTime.now();
        String channelCode = channel.getCode();
        if (channel.getCredentialStatus() == NewsCredentialStatus.INVALID) {
            writeRun(channelCode, runFeedId, triggerType, NewsRunStatus.FAILED, 0, "渠道凭证失效，跳过本次拉取。", startTime);
            return new ChannelRunOutcome(Collections.emptyList(), true);
        }
        List<Long> newItemIds = new ArrayList<>();
        List<FeedPullOutcome> feedOutcomes = new ArrayList<>();
        String abortReason = null;
        try {
            ChannelPlugin plugin = channelPluginRegistry.getPlugin(channelCode);
            ChannelCredential credential = prepareCredential(channelCode);
            boolean lockEachFeed = triggerType == NewsRunTriggerType.SCHEDULE;
            for (NewsFeedPO feed : feeds.get()) {
                feedOutcomes.add(lockEachFeed
                        ? pullFeedWithLock(plugin, feed, credential, newItemIds)
                        : pullFeed(plugin, feed, credential, newItemIds));
            }
        } catch (ChannelLoginExpiredException e) {
            log.warn("资讯渠道登录失效，中止拉取, channel={}, reason={}", channelCode, e.getMessage(), e);
            channelCredentialService.markInvalid(channelCode, e.getMessage());
            abortReason = String.format("登录凭证已失效：%s，已中止本次拉取。", reasonOf(e));
        } catch (RuntimeException e) {
            log.error("资讯渠道拉取失败, channel={}", channelCode, e);
            abortReason = String.format("渠道拉取失败：%s。", reasonOf(e));
        }
        boolean failed = abortReason != null || feedOutcomes.stream().anyMatch(FeedPullOutcome::isFailed);
        writeRun(channelCode, runFeedId, triggerType, failed ? NewsRunStatus.FAILED : NewsRunStatus.SUCCEEDED,
                newItemIds.size(), describeRun(abortReason, feedOutcomes, newItemIds.size()), startTime);
        return new ChannelRunOutcome(newItemIds, failed);
    }

    /**
     * 读取渠道凭证；插件能续期时先续期，得到新凭证就写回渠道表。
     *
     * @throws ChannelLoginExpiredException 凭证已失效或续期失败
     */
    private ChannelCredential prepareCredential(String channelCode) {
        ChannelCredential credential = channelCredentialService.loadCredential(channelCode);
        Optional<ChannelCredential> refreshed = channelPluginRegistry.findCapability(channelCode, CredentialRefreshable.class)
                .flatMap(refreshable -> refreshable.refreshIfNeeded(credential));
        refreshed.ifPresent(fresh -> channelCredentialService.saveCredential(channelCode, fresh, null));
        return refreshed.orElse(credential);
    }

    /**
     * 定时运行拉取一个订阅源：订阅源正在手动拉取时本次跳过。
     */
    private FeedPullOutcome pullFeedWithLock(ChannelPlugin plugin, NewsFeedPO feed, ChannelCredential credential,
                                             List<Long> newItemIds) {
        String lockKey = feedLockKey(feed.getId());
        if (!redisUtil.setIfAbsent(lockKey, FEED_LOCK_VALUE, FEED_LOCK_MINUTES, TimeUnit.MINUTES)) {
            return FeedPullOutcome.skipped(feed.getDisplayName());
        }
        try {
            return pullFeed(plugin, feed, credential, newItemIds);
        } finally {
            redisUtil.delete(lockKey);
        }
    }

    /**
     * 拉取一个订阅源并入库。成功时状态置为正常、推进拉取进度；出错时状态置为拉取失败并保存原因。
     *
     * @param newItemIds 新入库的资讯主键，逐条追加，订阅源中途出错时已入库的部分照常交给数据加工
     * @throws ChannelLoginExpiredException 渠道登录失效，由渠道运行中止该渠道
     */
    private FeedPullOutcome pullFeed(ChannelPlugin plugin, NewsFeedPO feed, ChannelCredential credential,
                                     List<Long> newItemIds) {
        LocalDateTime since = Optional.ofNullable(feed.getCursorTime())
                .orElseGet(() -> LocalDateTime.now().minusDays(FIRST_PULL_DAYS));
        try {
            List<FetchedItem> items = plugin.fetch(FeedCursor.builder().sourceKey(feed.getSourceKey()).since(since).build(),
                    credential).getItems();
            for (FetchedItem item : items) {
                itemIngestService.ingest(plugin, feed, item).ifPresent(newItemIds::add);
            }
            LocalDateTime cursorTime = items.stream()
                    .map(FetchedItem::getFeedTime)
                    .max(Comparator.naturalOrder())
                    .orElse(feed.getCursorTime());
            newsFeedMapper.updatePullStateById(feed.getId(), NewsFeedPullStatus.OK, null, LocalDateTime.now(), cursorTime);
            return FeedPullOutcome.succeeded(feed.getDisplayName());
        } catch (ChannelLoginExpiredException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("资讯订阅源拉取失败, feedId={}, channel={}, sourceKey={}",
                    feed.getId(), feed.getChannelCode(), feed.getSourceKey(), e);
            String reason = reasonOf(e);
            newsFeedMapper.updatePullStateById(feed.getId(), NewsFeedPullStatus.ERROR,
                    StringUtils.abbreviate(reason, PULL_ERROR_MAX_LENGTH), feed.getLastPullTime(), feed.getCursorTime());
            return FeedPullOutcome.failed(feed.getDisplayName(), reason);
        }
    }

    /**
     * 新入库的资讯同步交给数据加工。
     */
    private void processNewItems(List<Long> newItemIds) {
        if (CollectionUtils.isNotEmpty(newItemIds)) {
            newsAiProcessService.processItems(newItemIds);
        }
    }

    /**
     * 运行详情：一句话概括拉取了几个订阅源、新增几条，以及中止原因、跳过和失败的订阅源。
     */
    private String describeRun(String abortReason, List<FeedPullOutcome> feedOutcomes, int newCount) {
        StringBuilder detail = new StringBuilder();
        long pulledCount = feedOutcomes.stream().filter(outcome -> !outcome.isSkipped()).count();
        if (abortReason != null) {
            detail.append(abortReason);
            if (newCount > 0) {
                detail.append(String.format("已新增 %d 条进入待审。", newCount));
            }
        } else if (feedOutcomes.isEmpty()) {
            detail.append("没有启用中的订阅源。");
        } else if (newCount > 0) {
            detail.append(String.format("拉取 %d 个订阅源，新增 %d 条进入待审。", pulledCount, newCount));
        } else {
            detail.append(String.format("拉取 %d 个订阅源，没有新内容。", pulledCount));
        }
        detail.append(feedOutcomes.stream()
                .filter(outcome -> outcome.isSkipped() || outcome.isFailed())
                .map(outcome -> outcome.isSkipped()
                        ? String.format("%s 正在拉取中，本次跳过。", outcome.getFeedName())
                        : String.format("%s 拉取失败：%s。", outcome.getFeedName(), outcome.getError()))
                .collect(Collectors.joining()));
        return detail.toString();
    }

    private void writeRun(String channelCode, Long feedId, NewsRunTriggerType triggerType, NewsRunStatus status,
                          int newCount, String detail, LocalDateTime startTime) {
        NewsChannelRunPO run = new NewsChannelRunPO();
        run.setChannelCode(channelCode);
        run.setFeedId(feedId);
        run.setTriggerType(triggerType);
        run.setStatus(status);
        run.setNewCount(newCount);
        run.setDetail(detail);
        run.setStartTime(startTime);
        run.setEndTime(LocalDateTime.now());
        newsChannelRunMapper.insert(run);
        log.info("资讯渠道运行结束, channel={}, feedId={}, trigger={}, status={}, newCount={}, detail={}",
                channelCode, feedId, triggerType, status, newCount, detail);
    }

    private NewsFeedPO selectFeed(Long feedId) {
        return Optional.ofNullable(newsFeedMapper.selectById(feedId))
                .orElseThrow(() -> new BusinessException("订阅源不存在: " + feedId));
    }

    private static String feedLockKey(Long feedId) {
        return String.format(FEED_LOCK_KEY, feedId);
    }

    /**
     * 写进运行详情和订阅源失败原因的可读原因。
     */
    private static String reasonOf(RuntimeException e) {
        return StringUtils.removeEnd(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()), "。");
    }

    /**
     * 一个渠道一次运行的结果。
     */
    @Getter
    @AllArgsConstructor
    private static class ChannelRunOutcome {

        /**
         * 新入库的资讯主键
         */
        private final List<Long> newItemIds;

        /**
         * 凭证失效、登录失效、渠道出错或有订阅源拉取失败
         */
        private final boolean failed;
    }

    /**
     * 一个订阅源一次拉取的结果。
     */
    @Getter
    @AllArgsConstructor
    private static class FeedPullOutcome {

        private final String feedName;

        /**
         * 订阅源正在手动拉取，本次跳过
         */
        private final boolean skipped;

        /**
         * 拉取失败的原因；成功或跳过时为空
         */
        private final String error;

        static FeedPullOutcome succeeded(String feedName) {
            return new FeedPullOutcome(feedName, false, null);
        }

        static FeedPullOutcome skipped(String feedName) {
            return new FeedPullOutcome(feedName, true, null);
        }

        static FeedPullOutcome failed(String feedName, String error) {
            return new FeedPullOutcome(feedName, false, error);
        }

        boolean isFailed() {
            return error != null;
        }
    }
}
