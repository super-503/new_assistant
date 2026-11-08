package com.zengame.ai.business.paper;

import com.zengame.ai.business.paper.domain.request.PaperSearchRequest;
import com.zengame.ai.business.paper.domain.vo.PaperSearchPageVO;
import com.zengame.ai.infrastructure.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/** 管理端的论文检索入口。 */
@RestController
@RequiredArgsConstructor
public class PaperSearchController {

    private final PaperSearchService paperSearchService;

    /**
     * @param request 关键词、论文来源与分页游标
     * @return 所选来源的论文题录，需管理员会话
     */
    @GetMapping("/ai/api/web/v1/papers/search")
    public Result<PaperSearchPageVO> search(@Valid PaperSearchRequest request) {
        return Result.ok(paperSearchService.search(request));
    }
}
