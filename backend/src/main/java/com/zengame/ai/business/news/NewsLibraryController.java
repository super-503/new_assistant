package com.zengame.ai.business.news;

import com.zengame.ai.business.news.library.NewsLibraryService;
import com.zengame.ai.business.news.library.domain.request.NewsLibraryPageQueryRequest;
import com.zengame.ai.business.news.library.domain.vo.NewsLibraryFilterOptionsVO;
import com.zengame.ai.business.news.library.domain.vo.NewsLibraryTreeVO;
import com.zengame.ai.business.news.review.domain.request.NewsFieldSaveRequest;
import com.zengame.ai.business.news.review.domain.vo.NewsItemCardVO;
import com.zengame.ai.business.news.review.domain.vo.NewsItemDetailVO;
import com.zengame.ai.infrastructure.common.Result;
import com.zengame.ai.infrastructure.common.ResultList;
import com.zengame.ai.infrastructure.login.LoginUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 资讯库页接口：分类树、分页、详情、修改标签字段。只涉及已通过的资讯，所有接口仅超管可用。
 */
@RestController
@RequiredArgsConstructor
public class NewsLibraryController {

    private final NewsLibraryService newsLibraryService;

    /**
     * @return 分类树：类型 → 渠道 → 订阅源，每级带数量
     */
    @GetMapping("/ai/api/web/v1/news/library/tree")
    public Result<NewsLibraryTreeVO> tree() {
        return Result.ok(newsLibraryService.tree());
    }

    /**
     * @return 筛选栏的标签字段
     */
    @GetMapping("/ai/api/web/v1/news/library/filter-options")
    public Result<NewsLibraryFilterOptionsVO> filterOptions() {
        return Result.ok(newsLibraryService.filterOptions());
    }

    /**
     * 资讯库分页。
     *
     * @param request 查询条件与分页参数
     * @return 一页列表项
     */
    @PostMapping("/ai/api/web/v1/news/library/page")
    public Result<ResultList<NewsItemCardVO>> page(@Valid @RequestBody NewsLibraryPageQueryRequest request) {
        return Result.ok(newsLibraryService.page(request));
    }

    /**
     * @param id 资讯
     * @return 资讯详情
     */
    @GetMapping("/ai/api/web/v1/news/library/items/{id}")
    public Result<NewsItemDetailVO> detail(@PathVariable Long id) {
        return Result.ok(newsLibraryService.detail(id));
    }

    /**
     * 修改已通过资讯的标签字段。
     *
     * @param id      资讯
     * @param request 修改内容
     * @return 修改后的详情
     */
    @PostMapping("/ai/api/web/v1/news/library/items/{id}/tags")
    public Result<NewsItemDetailVO> updateTags(@PathVariable Long id, @Valid @RequestBody NewsFieldSaveRequest request) {
        return Result.ok(newsLibraryService.updateTags(id, request, LoginUtils.getUserName()));
    }
}
