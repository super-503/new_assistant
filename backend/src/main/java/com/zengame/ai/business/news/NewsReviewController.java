package com.zengame.ai.business.news;

import com.zengame.ai.business.news.review.NewsReviewService;
import com.zengame.ai.business.news.review.domain.request.NewsBatchRejectRequest;
import com.zengame.ai.business.news.review.domain.request.NewsFieldSaveRequest;
import com.zengame.ai.business.news.review.domain.request.NewsItemIdsRequest;
import com.zengame.ai.business.news.review.domain.request.NewsRejectRequest;
import com.zengame.ai.business.news.review.domain.request.NewsReviewPageQueryRequest;
import com.zengame.ai.business.news.review.domain.vo.NewsBatchResultVO;
import com.zengame.ai.business.news.review.domain.vo.NewsItemDetailVO;
import com.zengame.ai.business.news.review.domain.vo.NewsRejectReasonOptionVO;
import com.zengame.ai.business.news.review.domain.vo.NewsReviewFilterOptionsVO;
import com.zengame.ai.business.news.review.domain.vo.NewsReviewPageVO;
import com.zengame.ai.infrastructure.common.Result;
import com.zengame.ai.infrastructure.login.LoginUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 资讯审核页接口：审核队列、详情、字段修改、通过 / 拒绝、批量、撤销、恢复、重新加工、单字段生成，以及导航角标的待审数量。
 * 所有接口仅超管可用。
 */
@RestController
@RequiredArgsConstructor
public class NewsReviewController {

    private final NewsReviewService newsReviewService;

    /**
     * 审核队列分页，同时返回待审和已拒绝的总数。
     *
     * @param request 查询条件与分页参数
     * @return 一页列表项及两个分类的总数
     */
    @PostMapping("/ai/api/web/v1/news/review/page")
    public Result<NewsReviewPageVO> page(@Valid @RequestBody NewsReviewPageQueryRequest request) {
        return Result.ok(newsReviewService.page(request));
    }

    /**
     * @return 筛选栏的渠道、订阅源和标签字段
     */
    @GetMapping("/ai/api/web/v1/news/review/filter-options")
    public Result<NewsReviewFilterOptionsVO> filterOptions() {
        return Result.ok(newsReviewService.filterOptions());
    }

    /**
     * @return 拒绝弹窗可选的拒绝理由
     */
    @GetMapping("/ai/api/web/v1/news/review/reject-reasons")
    public Result<List<NewsRejectReasonOptionVO>> rejectReasons() {
        return Result.ok(newsReviewService.rejectReasons());
    }

    /**
     * @return 待审数量，供导航角标
     */
    @GetMapping("/ai/api/web/v1/news/review/pending-count")
    public Result<Long> pendingCount() {
        return Result.ok(newsReviewService.pendingCount());
    }

    /**
     * @param id 资讯
     * @return 资讯详情
     */
    @GetMapping("/ai/api/web/v1/news/review/items/{id}")
    public Result<NewsItemDetailVO> detail(@PathVariable Long id) {
        return Result.ok(newsReviewService.detail(id));
    }

    /**
     * 保存待审资讯上人工修改的字段。
     *
     * @param id      资讯
     * @param request 修改内容
     * @return 保存后的详情
     */
    @PostMapping("/ai/api/web/v1/news/review/items/{id}/fields")
    public Result<NewsItemDetailVO> saveFields(@PathVariable Long id, @Valid @RequestBody NewsFieldSaveRequest request) {
        return Result.ok(newsReviewService.saveFields(id, request, LoginUtils.getUserName()));
    }

    /**
     * 通过一条待审资讯。
     *
     * @param id 资讯
     * @return 空
     */
    @PostMapping("/ai/api/web/v1/news/review/items/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        newsReviewService.approve(id, LoginUtils.getUserName());
        return Result.ok();
    }

    /**
     * 拒绝一条待审资讯。
     *
     * @param id      资讯
     * @param request 拒绝理由和补充说明
     * @return 空
     */
    @PostMapping("/ai/api/web/v1/news/review/items/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody NewsRejectRequest request) {
        newsReviewService.reject(id, request.getReasonId(), request.getNote(), LoginUtils.getUserName());
        return Result.ok();
    }

    /**
     * 重新加工一条待审资讯。
     *
     * @param id 资讯
     * @return 加工后的详情
     */
    @PostMapping("/ai/api/web/v1/news/review/items/{id}/reprocess")
    public Result<NewsItemDetailVO> reprocess(@PathVariable Long id) {
        return Result.ok(newsReviewService.reprocess(id, LoginUtils.getUserName()));
    }

    /**
     * 为一条待审资讯单独生成一个字段。
     *
     * @param id       资讯
     * @param fieldKey 字段标识
     * @return 生成后的详情
     */
    @PostMapping("/ai/api/web/v1/news/review/items/{id}/fields/{fieldKey}/generate")
    public Result<NewsItemDetailVO> generateField(@PathVariable Long id, @PathVariable String fieldKey) {
        return Result.ok(newsReviewService.generateField(id, fieldKey, LoginUtils.getUserName()));
    }

    /**
     * 批量通过。
     *
     * @param request 资讯
     * @return 成功数和失败项
     */
    @PostMapping("/ai/api/web/v1/news/review/batch-approve")
    public Result<NewsBatchResultVO> batchApprove(@Valid @RequestBody NewsItemIdsRequest request) {
        return Result.ok(newsReviewService.batchApprove(request.getIds(), LoginUtils.getUserName()));
    }

    /**
     * 以同一个理由批量拒绝。
     *
     * @param request 资讯、拒绝理由和补充说明
     * @return 成功数和失败项
     */
    @PostMapping("/ai/api/web/v1/news/review/batch-reject")
    public Result<NewsBatchResultVO> batchReject(@Valid @RequestBody NewsBatchRejectRequest request) {
        return Result.ok(newsReviewService.batchReject(request.getIds(), request.getReasonId(), request.getNote(),
                LoginUtils.getUserName()));
    }

    /**
     * 撤销刚做出的通过或拒绝。
     *
     * @param request 资讯
     * @return 成功数和失败项
     */
    @PostMapping("/ai/api/web/v1/news/review/undo")
    public Result<NewsBatchResultVO> undo(@Valid @RequestBody NewsItemIdsRequest request) {
        return Result.ok(newsReviewService.undo(request.getIds(), LoginUtils.getUserName()));
    }

    /**
     * 把已拒绝的资讯恢复到待审。
     *
     * @param request 资讯
     * @return 成功数和失败项
     */
    @PostMapping("/ai/api/web/v1/news/review/restore")
    public Result<NewsBatchResultVO> restore(@Valid @RequestBody NewsItemIdsRequest request) {
        return Result.ok(newsReviewService.restore(request.getIds(), LoginUtils.getUserName()));
    }
}
