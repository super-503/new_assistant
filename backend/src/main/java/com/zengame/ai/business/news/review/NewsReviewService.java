package com.zengame.ai.business.news.review;

import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import com.zengame.ai.business.news.processing.NewsAiProcessService;
import com.zengame.ai.business.news.review.cover.NewsItemConvert;
import com.zengame.ai.business.news.review.cover.NewsItemViewConvert;
import com.zengame.ai.business.news.review.domain.bo.NewsItemBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemQueryBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemViewContext;
import com.zengame.ai.business.news.review.domain.request.NewsFieldSaveRequest;
import com.zengame.ai.business.news.review.domain.request.NewsReviewPageQueryRequest;
import com.zengame.ai.business.news.review.domain.vo.NewsBatchFailureVO;
import com.zengame.ai.business.news.review.domain.vo.NewsBatchResultVO;
import com.zengame.ai.business.news.review.domain.vo.NewsChannelOptionVO;
import com.zengame.ai.business.news.review.domain.vo.NewsItemCardVO;
import com.zengame.ai.business.news.review.domain.vo.NewsItemDetailVO;
import com.zengame.ai.business.news.review.domain.vo.NewsRejectReasonOptionVO;
import com.zengame.ai.business.news.review.domain.vo.NewsReviewFilterOptionsVO;
import com.zengame.ai.business.news.review.domain.vo.NewsReviewPageVO;
import com.zengame.ai.infrastructure.common.ResultList;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.NewsItemMapper;
import com.zengame.ai.repository.ai.mapper.NewsRejectReasonMapper;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 资讯审核：审核队列查询、详情、字段修改、单条与批量的审核动作、重新加工、单字段生成、待审数量。
 */
@Service
@RequiredArgsConstructor
public class NewsReviewService {

    private final NewsItemMapper newsItemMapper;

    private final NewsRejectReasonMapper newsRejectReasonMapper;

    private final NewsItemViewAssembler newsItemViewAssembler;

    private final NewsReviewActionService newsReviewActionService;

    private final NewsAiProcessService newsAiProcessService;

    /**
     * 审核队列分页：待审或已拒绝，加工中的资讯不出现。同时返回两个分类的总数。
     *
     * @param request 查询条件与分页参数
     * @return 一页列表项及两个分类的总数
     * @throws BusinessException 分类不是待审或已拒绝；筛选的字段不是启用中的标签字段
     */
    public NewsReviewPageVO page(NewsReviewPageQueryRequest request) {
        NewsItemQueryBO query = NewsItemConvert.INSTANCE.toQuery(request.getQuery());
        if (query.getStatus() != NewsItemStatus.PENDING && query.getStatus() != NewsItemStatus.REJECTED) {
            throw new BusinessException("审核队列只有待审和已拒绝两个分类");
        }
        ResultList<NewsItemCardVO> page = newsItemViewAssembler.page(query, request.getBasePageInfo(),
                newsItemViewAssembler.loadContext());
        return new NewsReviewPageVO(newsItemMapper.countByStatus(NewsItemStatus.PENDING),
                newsItemMapper.countByStatus(NewsItemStatus.REJECTED), page.getList(), page.getPagination());
    }

    /**
     * @return 筛选栏的渠道、订阅源和标签字段
     */
    public NewsReviewFilterOptionsVO filterOptions() {
        NewsItemViewContext context = newsItemViewAssembler.loadContext();
        List<NewsChannelOptionVO> channels = context.getChannels().stream()
                .map(channel -> new NewsChannelOptionVO(channel.getCode(), channel.getName(),
                        NewsItemViewConvert.INSTANCE.toFeedOptions(feedsOf(context, channel.getCode()))))
                .collect(Collectors.toList());
        return new NewsReviewFilterOptionsVO(channels, NewsItemViewConvert.INSTANCE.toTagFields(context.tagFields()));
    }

    /**
     * @return 拒绝弹窗可选的拒绝理由：启用中，按排序
     */
    public List<NewsRejectReasonOptionVO> rejectReasons() {
        return NewsItemViewConvert.INSTANCE.toReasonOptions(newsRejectReasonMapper.selectEnabled());
    }

    /**
     * @param id 资讯
     * @return 资讯详情
     * @throws BusinessException 资讯不存在
     */
    public NewsItemDetailVO detail(Long id) {
        return newsItemViewAssembler.detail(newsItemViewAssembler.load(id));
    }

    /**
     * 保存待审资讯上人工修改的字段。
     *
     * @param id       资讯
     * @param request  修改内容
     * @param operator 操作人登录名
     * @return 保存后的详情
     */
    public NewsItemDetailVO saveFields(Long id, NewsFieldSaveRequest request, String operator) {
        newsReviewActionService.saveFields(id, NewsItemConvert.INSTANCE.toEdits(request.getFields()), operator);
        return detail(id);
    }

    /**
     * @param id       资讯
     * @param operator 操作人登录名
     */
    public void approve(Long id, String operator) {
        newsReviewActionService.approve(id, operator);
    }

    /**
     * @param id       资讯
     * @param reasonId 启用中的拒绝理由
     * @param note     补充说明，可为空
     * @param operator 操作人登录名
     */
    public void reject(Long id, Long reasonId, String note, String operator) {
        newsReviewActionService.reject(id, reasonId, note, operator);
    }

    /**
     * 批量通过，逐条执行，每条在自己的事务里。
     *
     * @param ids      资讯
     * @param operator 操作人登录名
     * @return 成功数和失败项
     */
    public NewsBatchResultVO batchApprove(List<Long> ids, String operator) {
        return runEach(ids, id -> newsReviewActionService.approve(id, operator));
    }

    /**
     * 以同一个理由批量拒绝，逐条执行，每条在自己的事务里。
     *
     * @param ids      资讯
     * @param reasonId 启用中的拒绝理由
     * @param note     补充说明，可为空
     * @param operator 操作人登录名
     * @return 成功数和失败项
     * @throws BusinessException 拒绝理由不存在或已停用
     */
    public NewsBatchResultVO batchReject(List<Long> ids, Long reasonId, String note, String operator) {
        newsReviewActionService.requireEnabledReason(reasonId);
        return runEach(ids, id -> newsReviewActionService.reject(id, reasonId, note, operator));
    }

    /**
     * 撤销刚做出的通过或拒绝，逐条执行。
     *
     * @param ids      资讯
     * @param operator 操作人登录名
     * @return 成功数和失败项
     */
    public NewsBatchResultVO undo(List<Long> ids, String operator) {
        return runEach(ids, id -> newsReviewActionService.undo(id, operator));
    }

    /**
     * 把已拒绝的资讯恢复到待审，逐条执行。
     *
     * @param ids      资讯
     * @param operator 操作人登录名
     * @return 成功数和失败项
     */
    public NewsBatchResultVO restore(List<Long> ids, String operator) {
        return runEach(ids, id -> newsReviewActionService.restore(id, operator));
    }

    /**
     * 重新加工一条待审资讯，人工修改过的字段保留。
     *
     * @param id       资讯
     * @param operator 操作人登录名
     * @return 加工后的详情
     */
    public NewsItemDetailVO reprocess(Long id, String operator) {
        newsAiProcessService.reprocess(id, operator);
        return detail(id);
    }

    /**
     * 为一条待审资讯单独生成一个字段，生成的值写回资讯。
     *
     * @param id       资讯
     * @param fieldKey 字段标识
     * @param operator 操作人登录名
     * @return 生成后的详情
     */
    public NewsItemDetailVO generateField(Long id, String fieldKey, String operator) {
        newsAiProcessService.generateField(id, fieldKey, operator);
        return detail(id);
    }

    /**
     * @return 待审数量
     */
    public long pendingCount() {
        return newsItemMapper.countByStatus(NewsItemStatus.PENDING);
    }

    /**
     * 逐条执行审核动作：业务校验失败的条目记为失败项，不影响其他条目。
     */
    private NewsBatchResultVO runEach(List<Long> ids, Consumer<Long> action) {
        int successCount = 0;
        List<NewsBatchFailureVO> failures = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(ids)) {
            try {
                action.accept(id);
                successCount++;
            } catch (BusinessException e) {
                String title = newsItemViewAssembler.findTitle(id).orElse(null);
                failures.add(new NewsBatchFailureVO(id, title, e.getMessage()));
            }
        }
        return new NewsBatchResultVO(successCount, failures);
    }

    private List<NewsFeedPO> feedsOf(NewsItemViewContext context, String channelCode) {
        return context.getFeeds().stream()
                .filter(feed -> feed.getChannelCode().equals(channelCode))
                .collect(Collectors.toList());
    }
}
