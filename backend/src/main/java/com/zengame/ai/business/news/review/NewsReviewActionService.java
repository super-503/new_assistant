package com.zengame.ai.business.news.review;

import com.zengame.ai.business.news.NewsAiFieldCatalog;
import com.zengame.ai.business.news.domain.enums.NewsItemAction;
import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import com.zengame.ai.business.news.review.cover.NewsItemConvert;
import com.zengame.ai.business.news.review.domain.bo.NewsFieldEditBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemLogDetailBO;
import com.zengame.ai.business.news.setting.NewsAiModelSettingService;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.NewsItemMapper;
import com.zengame.ai.repository.ai.mapper.NewsRejectReasonMapper;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import com.zengame.ai.repository.ai.po.NewsRejectReasonPO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 单条资讯的审核动作：保存字段、通过、拒绝、撤销、恢复到待审。每个动作在自己的事务里锁定资讯行后执行，并追加处理记录。
 */
@Service
@RequiredArgsConstructor
public class NewsReviewActionService {

    private final NewsItemMapper newsItemMapper;

    private final NewsAiFieldCatalog newsAiFieldCatalog;

    private final NewsAiModelSettingService newsAiModelSettingService;

    private final NewsRejectReasonMapper newsRejectReasonMapper;

    private final NewsItemFieldEditor newsItemFieldEditor;

    private final NewsItemLogRecorder newsItemLogRecorder;

    /**
     * 保存待审资讯上人工修改的字段。只能修改适用于该资讯、启用且允许人工修改的字段；保存后所有必填字段仍须有值。保存不代表通过。
     *
     * @param id       资讯
     * @param edits    修改内容
     * @param operator 操作人登录名
     * @throws BusinessException 资讯不存在或不在待审；字段不允许修改、值不合法、没有变化；必填字段为空
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveFields(Long id, List<NewsFieldEditBO> edits, String operator) {
        newsAiModelSettingService.requireConfigured();
        NewsItemBO item = lock(id);
        requireStatus(item, "修改字段", NewsItemStatus.PENDING);
        List<NewsAiFieldPO> applicable = applicableFields(item);
        List<NewsAiFieldPO> editable = applicable.stream().filter(NewsAiFieldPO::getEditable).collect(Collectors.toList());
        List<NewsAiFieldPO> changed = newsItemFieldEditor.apply(item, editable, edits);
        newsItemFieldEditor.checkRequired(item, applicable);
        newsItemMapper.updateById(NewsItemConvert.INSTANCE.toPO(item));
        newsItemFieldEditor.recordEdited(id, changed, operator);
    }

    /**
     * 通过一条待审资讯，进入资讯库。
     *
     * @param id       资讯
     * @param operator 操作人登录名
     * @throws BusinessException 资讯不存在或不在待审；必填字段为空，提示「请填写 X」或「请选择 X」
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, String operator) {
        NewsItemBO item = lock(id);
        requireStatus(item, "通过", NewsItemStatus.PENDING);
        if (newsAiModelSettingService.isConfigured()) {
            newsItemFieldEditor.checkRequired(item, applicableFields(item));
        }
        item.setStatus(NewsItemStatus.APPROVED);
        item.setReviewer(operator);
        item.setReviewTime(LocalDateTime.now());
        newsItemMapper.updateById(NewsItemConvert.INSTANCE.toPO(item));
        newsItemLogRecorder.record(id, NewsItemAction.APPROVED, operator, null);
    }

    /**
     * 拒绝一条待审资讯。
     *
     * @param id       资讯
     * @param reasonId 启用中的拒绝理由
     * @param note     补充说明，可为空
     * @param operator 操作人登录名
     * @throws BusinessException 资讯不存在或不在待审；拒绝理由不存在或已停用
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, Long reasonId, String note, String operator) {
        NewsRejectReasonPO reason = requireEnabledReason(reasonId);
        NewsItemBO item = lock(id);
        requireStatus(item, "拒绝", NewsItemStatus.PENDING);
        String rejectNote = StringUtils.trimToNull(note);
        item.setStatus(NewsItemStatus.REJECTED);
        item.setRejectReasonId(reason.getId());
        item.setRejectNote(rejectNote);
        item.setReviewer(operator);
        item.setReviewTime(LocalDateTime.now());
        newsItemMapper.updateById(NewsItemConvert.INSTANCE.toPO(item));

        NewsItemLogDetailBO detail = new NewsItemLogDetailBO();
        detail.setReasonId(reason.getId());
        detail.setReasonName(reason.getName());
        detail.setNote(rejectNote);
        newsItemLogRecorder.record(id, NewsItemAction.REJECTED, operator, detail);
    }

    /**
     * 撤销刚做出的通过或拒绝：资讯回到待审，清空审核信息。
     *
     * @param id       资讯
     * @param operator 操作人登录名
     * @throws BusinessException 资讯不存在或不是已通过、已拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public void undo(Long id, String operator) {
        NewsItemBO item = lock(id);
        requireStatus(item, "撤销", NewsItemStatus.APPROVED, NewsItemStatus.REJECTED);
        backToPending(item, operator);
    }

    /**
     * 把已拒绝的资讯恢复到待审，清空审核信息。恢复不等于通过。
     *
     * @param id       资讯
     * @param operator 操作人登录名
     * @throws BusinessException 资讯不存在或不是已拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id, String operator) {
        NewsItemBO item = lock(id);
        requireStatus(item, "恢复到待审", NewsItemStatus.REJECTED);
        backToPending(item, operator);
    }

    /**
     * @param reasonId 拒绝理由主键
     * @return 启用中的拒绝理由
     * @throws BusinessException 拒绝理由不存在或已停用
     */
    public NewsRejectReasonPO requireEnabledReason(Long reasonId) {
        return Optional.ofNullable(newsRejectReasonMapper.selectById(reasonId))
                .filter(NewsRejectReasonPO::getEnabled)
                .orElseThrow(() -> new BusinessException("拒绝理由不存在或已停用: " + reasonId));
    }

    /**
     * 资讯回到待审，清空审核信息，追加 RESTORED 处理记录。
     */
    private void backToPending(NewsItemBO item, String operator) {
        item.setStatus(NewsItemStatus.PENDING);
        item.setRejectReasonId(null);
        item.setRejectNote(null);
        item.setReviewer(null);
        item.setReviewTime(null);
        newsItemMapper.updateById(NewsItemConvert.INSTANCE.toPO(item));
        newsItemLogRecorder.record(item.getId(), NewsItemAction.RESTORED, operator, null);
    }

    /**
     * 在当前事务内锁定并读取资讯。
     *
     * @throws BusinessException 资讯不存在
     */
    private NewsItemBO lock(Long id) {
        return Optional.ofNullable(newsItemMapper.selectByIdForUpdate(id))
                .map(NewsItemConvert.INSTANCE::toBO)
                .orElseThrow(() -> new BusinessException("资讯不存在: " + id));
    }

    /**
     * @throws BusinessException 资讯状态不在允许范围内，如「资讯当前为「已拒绝」，不能通过」
     */
    private void requireStatus(NewsItemBO item, String actionName, NewsItemStatus... allowed) {
        if (!Arrays.asList(allowed).contains(item.getStatus())) {
            throw new BusinessException("资讯当前为「" + item.getStatus().getDesc() + "」，不能" + actionName);
        }
    }

    /**
     * @return 对这条资讯适用的启用字段，按展示顺序
     */
    private List<NewsAiFieldPO> applicableFields(NewsItemBO item) {
        return newsAiFieldCatalog.listEnabled().stream()
                .filter(field -> NewsAiFieldRules.isApplicable(field, item.getContentType()))
                .collect(Collectors.toList());
    }
}
