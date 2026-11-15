package com.zengame.ai.business.news.setting;

import com.zengame.ai.business.news.library.NewsFeishuDigestService;
import com.zengame.ai.business.news.setting.cover.NewsPushPlanConvert;
import com.zengame.ai.business.news.setting.cover.NewsDigestPreviewConvert;
import com.zengame.ai.business.news.setting.domain.bo.NewsDigestContentSettings;
import com.zengame.ai.business.news.setting.domain.bo.NewsPushPlanDefinition;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuDigestDelivery;
import com.zengame.ai.business.news.setting.domain.enums.NewsPushPlanBatchResult;
import com.zengame.ai.business.news.setting.domain.request.NewsPushPlanPreviewRequest;
import com.zengame.ai.business.news.setting.domain.request.NewsPushPlanSaveRequest;
import com.zengame.ai.business.news.setting.domain.vo.NewsPushPlanListVO;
import com.zengame.ai.business.news.setting.domain.vo.NewsPushPlanVO;
import com.zengame.ai.business.news.setting.domain.vo.NewsDigestPreviewItemVO;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.mapper.NewsPushPlanMapper;
import com.zengame.ai.repository.ai.po.NewsPushPlanPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** 管理推送计划并按计划时区运行已经到期的投递。 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NewsPushPlanService {
    private static final int RETRY_MINUTES = 5;
    private final NewsPushPlanMapper plans;
    private final NewsProperties properties;
    private final NewsFeishuNotificationSettingService feishu;
    private final NewsFeishuDigestService digest;

    /** @return 可编辑的推送计划和显示时区 */
    public NewsPushPlanListVO list() {
        return new NewsPushPlanListVO(properties.getTimeZone(), plans.selectAll().stream()
                .map(NewsPushPlanConvert.INSTANCE::toVO).collect(Collectors.toList()));
    }

    /** @return 新建的推送计划 */
    public NewsPushPlanVO create(NewsPushPlanSaveRequest request) {
        var definition = request.getDefinition();
        feishu.validatePlanDestination(definition);
        var plan = new NewsPushPlanPO();
        apply(plan, request, request.isEnabled() ? nextRun(definition, Instant.now()) : null);
        plans.insert(plan);
        return NewsPushPlanConvert.INSTANCE.toVO(plan);
    }

    /**
     * @param id 推送计划主键
     * @param request 编辑后的计划及版本
     * @return 已保存的计划
     * @throws BusinessException 计划不存在、版本已变更或发送目标不可用
     */
    @Transactional(rollbackFor = Exception.class)
    public NewsPushPlanVO update(long id, NewsPushPlanSaveRequest request) {
        var plan = Optional.ofNullable(plans.selectForUpdate(id))
                .orElseThrow(() -> new BusinessException("推送计划不存在"));
        if (!Objects.equals(request.getRevision(), plan.getRevision())) {
            throw new BusinessException("推送计划已更新，请刷新后重试");
        }
        var updated = request.getDefinition();
        // 已停用的计划可在渠道配置变化后继续保持停用。
        if (request.isEnabled()) feishu.validatePlanDestination(updated);
        LocalDateTime next = plan.getNextRunAt();
        if (!request.isEnabled()) next = null;
        else if (!plan.isEnabled() || !updated.getSendTime().equals(definition(plan).getSendTime())) {
            next = nextRun(updated, Instant.now());
        }
        apply(plan, request, next);
        plan.setRevision(plan.getRevision() + 1);
        plans.update(plan);
        return NewsPushPlanConvert.INSTANCE.toVO(plan);
    }

    /** @return 当前到期且仍启用的计划主键 */
    public List<Long> due() {
        return plans.selectDue(LocalDateTime.now(ZoneOffset.UTC));
    }

    /**
     * @param request 计划草稿及其中一位接收人
     * @throws BusinessException 计划目标无效、没有可预览资讯或飞书拒绝发送
     */
    public void preview(NewsPushPlanPreviewRequest request) {
        feishu.verifyPlan(request);
    }

    /**
     * @param content 资讯范围和显示字段
     * @return 与发送格式一致的已审核资讯预览，范围内暂无资讯时返回空列表
     */
    public List<NewsDigestPreviewItemVO> previewContent(NewsDigestContentSettings content) {
        return NewsDigestPreviewConvert.INSTANCE.toVO(digest.previewEntries(content));
    }

    /**
     * @param id 候选投递计划
     * @return 是否需要继续同一次运行的下一批
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public boolean runDueBatch(long id) {
        var now = Instant.now();
        var plan = plans.selectForUpdate(id);
        if (plan == null || !plan.isEnabled() || plan.getNextRunAt() == null
                || plan.getNextRunAt().isAfter(LocalDateTime.ofInstant(now, ZoneOffset.UTC))) return false;

        var definition = definition(plan);
        var result = sendDueBatch(id, definition);
        if (result == NewsPushPlanBatchResult.MORE) return true;
        if (result == NewsPushPlanBatchResult.COMPLETE) {
            plans.updateNextRunAt(id, nextRun(definition, Instant.now()));
            return false;
        }
        plans.updateNextRunAt(id, LocalDateTime.now(ZoneOffset.UTC).plusMinutes(RETRY_MINUTES));
        return false;
    }

    /** @return 计划的本批发送结果，渠道停用或目标配置失效时等待补发 */
    private NewsPushPlanBatchResult sendDueBatch(long id, NewsPushPlanDefinition definition) {
        NewsFeishuDigestDelivery delivery;
        try {
            delivery = feishu.planDigestDelivery(definition);
        } catch (BusinessException e) {
            log.warn("推送计划等待配置修复后补发, planId={}", id, e);
            return NewsPushPlanBatchResult.RETRY;
        }
        if (CollectionUtils.isEmpty(delivery.getTargets())) return NewsPushPlanBatchResult.RETRY;
        return digest.sendPlanBatchResult(id, delivery);
    }

    /** @return 下一次在计划时区达到同一时刻的 UTC 时间 */
    private LocalDateTime nextRun(NewsPushPlanDefinition definition, Instant now) {
        ZoneId zone = ZoneId.of(properties.getTimeZone());
        LocalTime time = LocalTime.parse(definition.getSendTime());
        LocalDate day = now.atZone(zone).toLocalDate();
        var candidate = day.atTime(time).atZone(zone).toInstant();
        if (!candidate.isAfter(now)) candidate = day.plusDays(1).atTime(time).atZone(zone).toInstant();
        return LocalDateTime.ofInstant(candidate, ZoneOffset.UTC);
    }

    /** 将计划配置及对应的下一次发送时间保存为同一状态。 */
    private void apply(NewsPushPlanPO plan, NewsPushPlanSaveRequest request, LocalDateTime next) {
        plan.setConfigJson(JacksonUtil.writeValueAsString(request.getDefinition()));
        plan.setEnabled(request.isEnabled());
        plan.setNextRunAt(next);
    }

    /** @return 计划的保存配置 */
    private NewsPushPlanDefinition definition(NewsPushPlanPO plan) {
        return NewsPushPlanConvert.INSTANCE.readDefinition(plan.getConfigJson());
    }
}
