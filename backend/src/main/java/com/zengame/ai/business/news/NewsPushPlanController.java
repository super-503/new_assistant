package com.zengame.ai.business.news;

import com.zengame.ai.business.news.setting.NewsPushPlanService;
import com.zengame.ai.business.news.setting.domain.bo.NewsDigestContentSettings;
import com.zengame.ai.business.news.setting.domain.request.NewsPushPlanSaveRequest;
import com.zengame.ai.business.news.setting.domain.request.NewsPushPlanPreviewRequest;
import com.zengame.ai.business.news.setting.domain.vo.NewsPushPlanListVO;
import com.zengame.ai.business.news.setting.domain.vo.NewsPushPlanVO;
import com.zengame.ai.business.news.setting.domain.vo.NewsDigestPreviewItemVO;
import com.zengame.ai.infrastructure.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/** 管理资讯推送计划的接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/api/web/v1/news/setting/push-plans")
public class NewsPushPlanController {
    private final NewsPushPlanService plans;

    /** @return 推送计划列表和计划时区 */
    @GetMapping
    public Result<NewsPushPlanListVO> list() {
        return Result.ok(plans.list());
    }

    /** @return 已保存的新推送计划 */
    @PostMapping
    public Result<NewsPushPlanVO> create(@Valid @RequestBody NewsPushPlanSaveRequest request) {
        return Result.ok(plans.create(request));
    }

    /** @return 已保存的推送计划 */
    @PutMapping("/{id}")
    public Result<NewsPushPlanVO> update(@PathVariable long id, @Valid @RequestBody NewsPushPlanSaveRequest request) {
        return Result.ok(plans.update(id, request));
    }

    /** 向计划中指定的接收人试发最多三条已审核资讯。 */
    @PostMapping("/preview")
    public Result<Void> preview(@Valid @RequestBody NewsPushPlanPreviewRequest request) {
        plans.preview(request);
        return Result.ok();
    }

    /** @return 按当前范围、标题和显示字段生成的真实资讯布局预览 */
    @PostMapping("/content-preview")
    public Result<List<NewsDigestPreviewItemVO>> previewContent(@Valid @RequestBody NewsDigestContentSettings request) {
        return Result.ok(plans.previewContent(request));
    }
}
