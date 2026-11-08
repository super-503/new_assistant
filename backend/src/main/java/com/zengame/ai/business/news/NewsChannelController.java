package com.zengame.ai.business.news;

import com.zengame.ai.business.news.channel.ChannelCredentialService;
import com.zengame.ai.business.news.feed.NewsChannelService;
import com.zengame.ai.business.news.feed.NewsFeedService;
import com.zengame.ai.business.news.feed.domain.request.EnabledSaveRequest;
import com.zengame.ai.business.news.feed.domain.request.FeedCreateRequest;
import com.zengame.ai.business.news.feed.domain.request.FeedUpdateRequest;
import com.zengame.ai.business.news.feed.domain.request.QrLoginPollRequest;
import com.zengame.ai.business.news.feed.domain.request.SourcePreviewRequest;
import com.zengame.ai.business.news.feed.domain.request.SourceSearchRequest;
import com.zengame.ai.business.news.feed.domain.request.TokenSaveRequest;
import com.zengame.ai.business.news.feed.domain.vo.ChannelRunVO;
import com.zengame.ai.business.news.feed.domain.vo.ChannelTodoVO;
import com.zengame.ai.business.news.feed.domain.vo.ChannelVO;
import com.zengame.ai.business.news.feed.domain.vo.CredentialVerifyVO;
import com.zengame.ai.business.news.feed.domain.vo.FeedVO;
import com.zengame.ai.business.news.feed.domain.vo.QrLoginPollVO;
import com.zengame.ai.business.news.feed.domain.vo.QrLoginStartVO;
import com.zengame.ai.business.news.feed.domain.vo.SourceCandidateVO;
import com.zengame.ai.business.news.feed.domain.vo.SourcePreviewVO;
import com.zengame.ai.infrastructure.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 资讯中心「渠道配置」页接口：渠道列表、渠道设置、凭证维护、订阅源管理、运行记录、「需处理」汇总。
 * 所有接口仅超管可用。
 */
@RestController
@RequiredArgsConstructor
public class NewsChannelController {

    private final NewsChannelService newsChannelService;

    private final NewsFeedService newsFeedService;

    private final ChannelCredentialService channelCredentialService;

    /**
     * 渠道列表，带凭证状态、订阅源数量和是否需要处理。
     *
     * @return 全部渠道
     */
    @GetMapping("/ai/api/web/v1/news/channels")
    public Result<List<ChannelVO>> listChannels() {
        return Result.ok(newsChannelService.listChannels());
    }

    /**
     * 保存渠道设置：启用或停用渠道。
     *
     * @param channelCode 渠道标识
     * @param request     是否启用
     * @return 空结果
     */
    @PostMapping("/ai/api/web/v1/news/channels/{channelCode}/settings")
    public Result<Void> saveChannelSettings(@PathVariable String channelCode,
                                            @Valid @RequestBody EnabledSaveRequest request) {
        newsChannelService.updateChannelEnabled(channelCode, request.getEnabled());
        return Result.ok();
    }

    /**
     * 验证连接：用渠道当前保存的凭证查询登录账号。
     *
     * @param channelCode 渠道标识
     * @return 是否可用、登录账号或失败原因
     */
    @PostMapping("/ai/api/web/v1/news/channels/{channelCode}/credential/verify")
    public Result<CredentialVerifyVO> verifyCredential(@PathVariable String channelCode) {
        return Result.ok(channelCredentialService.verifyCredential(channelCode));
    }

    /**
     * 发起扫码登录。
     *
     * @param channelCode 渠道标识
     * @return 扫码会话标识、二维码、有效期
     */
    @PostMapping("/ai/api/web/v1/news/channels/{channelCode}/credential/qr/start")
    public Result<QrLoginStartVO> startQrLogin(@PathVariable String channelCode) {
        return Result.ok(channelCredentialService.startQrLogin(channelCode));
    }

    /**
     * 轮询一次扫码结果；确认登录时凭证已保存。
     *
     * @param channelCode 渠道标识
     * @param request     扫码会话标识
     * @return 扫码状态
     */
    @PostMapping("/ai/api/web/v1/news/channels/{channelCode}/credential/qr/poll")
    public Result<QrLoginPollVO> pollQrLogin(@PathVariable String channelCode,
                                             @Valid @RequestBody QrLoginPollRequest request) {
        return Result.ok(channelCredentialService.pollQrLogin(channelCode, request.getSessionId()));
    }

    /**
     * 保存 Token：校验通过才保存，返回保存后的渠道（带登录账号）。
     *
     * @param channelCode 渠道标识
     * @param request     Token
     * @return 保存后的渠道
     */
    @PostMapping("/ai/api/web/v1/news/channels/{channelCode}/credential/token")
    public Result<ChannelVO> saveToken(@PathVariable String channelCode,
                                       @Valid @RequestBody TokenSaveRequest request) {
        channelCredentialService.saveToken(channelCode, request.getToken());
        return Result.ok(newsChannelService.getChannel(channelCode));
    }

    /**
     * 校验输入框里的 Token，不保存，不改变渠道凭证状态。
     *
     * @param channelCode 渠道标识
     * @param request     Token
     * @return 是否可用、登录账号或失败原因
     */
    @PostMapping("/ai/api/web/v1/news/channels/{channelCode}/credential/token/verify")
    public Result<CredentialVerifyVO> verifyToken(@PathVariable String channelCode,
                                                  @Valid @RequestBody TokenSaveRequest request) {
        return Result.ok(channelCredentialService.verifyToken(channelCode, request.getToken()));
    }

    /**
     * 渠道最近 50 次运行记录。
     *
     * @param channelCode 渠道标识
     * @return 运行记录，按开始时间倒序
     */
    @GetMapping("/ai/api/web/v1/news/channels/{channelCode}/runs")
    public Result<List<ChannelRunVO>> listRuns(@PathVariable String channelCode) {
        return Result.ok(newsChannelService.listRuns(channelCode));
    }

    /**
     * 「需处理」汇总。
     *
     * @return 凭证失效的渠道和拉取失败的订阅源
     */
    @GetMapping("/ai/api/web/v1/news/todo")
    public Result<ChannelTodoVO> todo() {
        return Result.ok(newsChannelService.todo());
    }

    /**
     * 渠道下的订阅源列表。
     *
     * @param channelCode 渠道标识
     * @param status      状态筛选：OK / ERROR / PENDING / DISABLED；不传不筛选
     * @param keyword     显示名称关键词；不传不筛选
     * @return 订阅源表格行
     */
    @GetMapping("/ai/api/web/v1/news/channels/{channelCode}/feeds")
    public Result<List<FeedVO>> listFeeds(@PathVariable String channelCode,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String keyword) {
        return Result.ok(newsFeedService.listFeeds(channelCode, status, keyword));
    }

    /**
     * 订阅一个从搜索结果里选中的来源。
     *
     * @param channelCode 渠道标识
     * @param request     来源信息、显示名称、内容类型、是否启用
     * @return 空结果
     */
    @PostMapping("/ai/api/web/v1/news/channels/{channelCode}/feeds")
    public Result<Void> createFeed(@PathVariable String channelCode,
                                   @Valid @RequestBody FeedCreateRequest request) {
        newsFeedService.createFeed(channelCode, request);
        return Result.ok();
    }

    /**
     * 在渠道内搜索来源；不支持搜索的渠道直接返回唯一的来源。
     *
     * @param channelCode 渠道标识
     * @param request     关键词
     * @return 搜索结果
     */
    @PostMapping("/ai/api/web/v1/news/channels/{channelCode}/sources/search")
    public Result<List<SourceCandidateVO>> searchSources(@PathVariable String channelCode,
                                                         @RequestBody SourceSearchRequest request) {
        return Result.ok(newsFeedService.searchSources(channelCode, request.getKeyword()));
    }

    /**
     * 预览一个来源。
     *
     * @param channelCode 渠道标识
     * @param request     来源标识
     * @return 来源卡片
     */
    @PostMapping("/ai/api/web/v1/news/channels/{channelCode}/sources/preview")
    public Result<SourcePreviewVO> previewSource(@PathVariable String channelCode,
                                                 @Valid @RequestBody SourcePreviewRequest request) {
        return Result.ok(newsFeedService.previewSource(channelCode, request.getSourceKey()));
    }

    /**
     * 编辑订阅源：显示名称、内容类型、是否启用。
     *
     * @param feedId  订阅源 id
     * @param request 新的设置
     * @return 空结果
     */
    @PostMapping("/ai/api/web/v1/news/feeds/{feedId}")
    public Result<Void> updateFeed(@PathVariable Long feedId, @Valid @RequestBody FeedUpdateRequest request) {
        newsFeedService.updateFeed(feedId, request);
        return Result.ok();
    }

    /**
     * 订阅源行内的启用开关。
     *
     * @param feedId  订阅源 id
     * @param request 是否启用
     * @return 空结果
     */
    @PostMapping("/ai/api/web/v1/news/feeds/{feedId}/enabled")
    public Result<Void> updateFeedEnabled(@PathVariable Long feedId, @Valid @RequestBody EnabledSaveRequest request) {
        newsFeedService.updateFeedEnabled(feedId, request.getEnabled());
        return Result.ok();
    }

    /**
     * 删除订阅源，已入库的资讯保留。
     *
     * @param feedId 订阅源 id
     * @return 空结果
     */
    @PostMapping("/ai/api/web/v1/news/feeds/{feedId}/delete")
    public Result<Void> deleteFeed(@PathVariable Long feedId) {
        newsFeedService.deleteFeed(feedId);
        return Result.ok();
    }

    /**
     * 立即拉取订阅源，加入拉取队列后异步执行。
     *
     * @param feedId 订阅源 id
     * @return 空结果
     */
    @PostMapping("/ai/api/web/v1/news/feeds/{feedId}/pull")
    public Result<Void> pullFeed(@PathVariable Long feedId) {
        newsFeedService.pullFeed(feedId);
        return Result.ok();
    }

    /**
     * 重试拉取失败的订阅源，加入拉取队列后异步执行；渠道凭证失效时提示「重试失败：渠道凭证失效」。
     *
     * @param feedId 订阅源 id
     * @return 空结果
     */
    @PostMapping("/ai/api/web/v1/news/feeds/{feedId}/retry")
    public Result<Void> retryFeed(@PathVariable Long feedId) {
        newsFeedService.pullFeed(feedId);
        return Result.ok();
    }
}
