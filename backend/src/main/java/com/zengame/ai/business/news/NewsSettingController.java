package com.zengame.ai.business.news;

import com.zengame.ai.business.news.setting.NewsAiFieldSettingService;
import com.zengame.ai.business.news.setting.NewsAiModelSettingService;
import com.zengame.ai.business.news.setting.NewsFeishuNotificationSettingService;
import com.zengame.ai.business.news.setting.NewsRejectReasonSettingService;
import com.zengame.ai.business.news.setting.domain.request.NewsAiFieldSaveRequest;
import com.zengame.ai.business.news.setting.domain.request.NewsAiFieldSortRequest;
import com.zengame.ai.business.news.setting.domain.request.NewsAiModelSaveRequest;
import com.zengame.ai.business.news.setting.domain.request.NewsEnabledRequest;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuNotificationSaveRequest;
import com.zengame.ai.business.news.setting.domain.request.NewsRejectReasonSaveRequest;
import com.zengame.ai.business.news.setting.domain.vo.NewsAiFieldVO;
import com.zengame.ai.business.news.setting.domain.vo.NewsAiModelSettingVO;
import com.zengame.ai.business.news.setting.domain.vo.NewsFeishuNotificationSettingVO;
import com.zengame.ai.business.news.setting.domain.vo.NewsRejectReasonVO;
import com.zengame.ai.infrastructure.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 资讯中心「基础配置」页接口：模型、AI 字段、拒绝理由和通知渠道。只有超管可以调用。
 */
@RestController
@RequiredArgsConstructor
public class NewsSettingController {

    private static final String PREFIX = "/ai/api/web/v1/news/setting";

    private final NewsAiFieldSettingService newsAiFieldSettingService;

    private final NewsAiModelSettingService newsAiModelSettingService;
    private final NewsFeishuNotificationSettingService newsFeishuNotificationSettingService;

    private final NewsRejectReasonSettingService newsRejectReasonSettingService;

    /** @return 管理员可查看的模型配置及密钥状态 */
    @GetMapping(PREFIX + "/ai-model")
    public Result<NewsAiModelSettingVO> getAiModel() {
        return Result.ok(newsAiModelSettingService.get());
    }

    /**
     * 保存资讯加工使用的模型。
     * @param request 模型配置
     * @return 保存后的模型状态
     */
    @PutMapping(PREFIX + "/ai-model")
    public Result<NewsAiModelSettingVO> saveAiModel(@Valid @RequestBody NewsAiModelSaveRequest request) {
        return Result.ok(newsAiModelSettingService.save(request));
    }

    /**
     * 用表单内容测试模型连接。
     * @param request 待测试的模型配置
     * @return 测试成功
     */
    @PostMapping(PREFIX + "/ai-model/verify")
    public Result<Void> verifyAiModel(@Valid @RequestBody NewsAiModelSaveRequest request) {
        newsAiModelSettingService.verify(request);
        return Result.ok();
    }

    /** @return 管理员可查看的飞书应用机器人配置状态 */
    @GetMapping(PREFIX + "/notification-channel")
    public Result<NewsFeishuNotificationSettingVO> getNotificationChannel() {
        return Result.ok(newsFeishuNotificationSettingService.get());
    }

    /**
     * 保存个人日报的飞书单聊通知配置。
     * @param request 飞书应用机器人配置
     * @return 保存后的通知配置状态
     */
    @PutMapping(PREFIX + "/notification-channel")
    public Result<NewsFeishuNotificationSettingVO> saveNotificationChannel(
            @Valid @RequestBody NewsFeishuNotificationSaveRequest request) {
        return Result.ok(newsFeishuNotificationSettingService.save(request));
    }

    /**
     * 使用表单配置向指定接收人发送一条飞书单聊测试消息。
     * @param request 待测试的配置
     * @return 飞书确认接收
     */
    @PostMapping(PREFIX + "/notification-channel/verify")
    public Result<Void> verifyNotificationChannel(@Valid @RequestBody NewsFeishuNotificationSaveRequest request) {
        newsFeishuNotificationSettingService.verify(request);
        return Result.ok();
    }

    /**
     * @return 当前可配置的 AI 字段，按展示顺序
     */
    @GetMapping(PREFIX + "/ai-fields")
    public Result<List<NewsAiFieldVO>> listAiFields() {
        return Result.ok(newsAiFieldSettingService.list());
    }

    /**
     * 新增普通 AI 字段。
     *
     * @param request 字段配置
     * @return 新字段的主键
     */
    @PostMapping(PREFIX + "/ai-fields")
    public Result<Long> createAiField(@Valid @RequestBody NewsAiFieldSaveRequest request) {
        return Result.ok(newsAiFieldSettingService.create(request));
    }

    /**
     * 编辑 AI 字段，锁定项须与当前值一致。
     *
     * @param id      字段主键
     * @param request 字段配置
     * @return 空结果
     */
    @PutMapping(PREFIX + "/ai-fields/{id}")
    public Result<Void> updateAiField(@PathVariable Long id, @Valid @RequestBody NewsAiFieldSaveRequest request) {
        newsAiFieldSettingService.update(id, request);
        return Result.ok();
    }

    /**
     * 删除普通 AI 字段。
     *
     * @param id 字段主键
     * @return 空结果
     */
    @DeleteMapping(PREFIX + "/ai-fields/{id}")
    public Result<Void> deleteAiField(@PathVariable Long id) {
        newsAiFieldSettingService.delete(id);
        return Result.ok();
    }

    /**
     * 启用或停用 AI 字段，内置字段不可操作。
     *
     * @param id      字段主键
     * @param request 是否启用
     * @return 空结果
     */
    @PutMapping(PREFIX + "/ai-fields/{id}/enabled")
    public Result<Void> setAiFieldEnabled(@PathVariable Long id, @Valid @RequestBody NewsEnabledRequest request) {
        newsAiFieldSettingService.setEnabled(id, request.getEnabled());
        return Result.ok();
    }

    /**
     * 按传入的完整顺序重排 AI 字段。
     *
     * @param request 当前可配置的字段主键，按新的展示顺序
     * @return 空结果
     */
    @PutMapping(PREFIX + "/ai-fields/sort")
    public Result<Void> sortAiFields(@Valid @RequestBody NewsAiFieldSortRequest request) {
        newsAiFieldSettingService.sort(request.getIds());
        return Result.ok();
    }

    /**
     * @return 全部拒绝理由，按排序
     */
    @GetMapping(PREFIX + "/reject-reasons")
    public Result<List<NewsRejectReasonVO>> listRejectReasons() {
        return Result.ok(newsRejectReasonSettingService.list());
    }

    /**
     * 新增拒绝理由。
     *
     * @param request 理由配置
     * @return 新理由的主键
     */
    @PostMapping(PREFIX + "/reject-reasons")
    public Result<Long> createRejectReason(@Valid @RequestBody NewsRejectReasonSaveRequest request) {
        return Result.ok(newsRejectReasonSettingService.create(request));
    }

    /**
     * 编辑拒绝理由。
     *
     * @param id      理由主键
     * @param request 理由配置
     * @return 空结果
     */
    @PutMapping(PREFIX + "/reject-reasons/{id}")
    public Result<Void> updateRejectReason(@PathVariable Long id, @Valid @RequestBody NewsRejectReasonSaveRequest request) {
        newsRejectReasonSettingService.update(id, request);
        return Result.ok();
    }

    /**
     * 启用或停用拒绝理由。
     *
     * @param id      理由主键
     * @param request 是否启用
     * @return 空结果
     */
    @PutMapping(PREFIX + "/reject-reasons/{id}/enabled")
    public Result<Void> setRejectReasonEnabled(@PathVariable Long id, @Valid @RequestBody NewsEnabledRequest request) {
        newsRejectReasonSettingService.setEnabled(id, request.getEnabled());
        return Result.ok();
    }
}
