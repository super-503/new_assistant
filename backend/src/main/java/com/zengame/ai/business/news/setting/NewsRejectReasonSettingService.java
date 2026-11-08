package com.zengame.ai.business.news.setting;

import com.zengame.ai.business.news.setting.cover.NewsRejectReasonConvert;
import com.zengame.ai.business.news.setting.domain.request.NewsRejectReasonSaveRequest;
import com.zengame.ai.business.news.setting.domain.vo.NewsRejectReasonVO;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.NewsRejectReasonMapper;
import com.zengame.ai.repository.ai.po.NewsRejectReasonPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 拒绝理由配置：新增、编辑、启停。停用的理由不再出现在拒绝弹窗里，已拒绝资讯上的理由保留。
 */
@Service
@RequiredArgsConstructor
public class NewsRejectReasonSettingService {

    private final NewsRejectReasonMapper newsRejectReasonMapper;

    /**
     * @return 全部拒绝理由，按排序
     */
    public List<NewsRejectReasonVO> list() {
        return NewsRejectReasonConvert.INSTANCE.toVOList(newsRejectReasonMapper.selectAll());
    }

    /**
     * 新增拒绝理由，排在最后。
     *
     * @param request 理由配置
     * @return 新理由的主键
     */
    public Long create(NewsRejectReasonSaveRequest request) {
        NewsRejectReasonPO reason = NewsRejectReasonConvert.INSTANCE.toPO(request);
        reason.setSortNo(nextSortNo());
        newsRejectReasonMapper.insert(reason);
        return reason.getId();
    }

    /**
     * 编辑拒绝理由。
     *
     * @param id      理由主键
     * @param request 理由配置
     * @throws BusinessException 理由不存在
     */
    public void update(Long id, NewsRejectReasonSaveRequest request) {
        NewsRejectReasonPO reason = load(id);
        NewsRejectReasonConvert.INSTANCE.updatePO(request, reason);
        newsRejectReasonMapper.updateById(reason);
    }

    /**
     * 启用或停用拒绝理由。
     *
     * @param id      理由主键
     * @param enabled 是否启用
     * @throws BusinessException 理由不存在
     */
    public void setEnabled(Long id, boolean enabled) {
        NewsRejectReasonPO reason = load(id);
        reason.setEnabled(enabled);
        newsRejectReasonMapper.updateById(reason);
    }

    private NewsRejectReasonPO load(Long id) {
        return Optional.ofNullable(newsRejectReasonMapper.selectById(id))
                .orElseThrow(() -> new BusinessException("拒绝理由不存在: " + id));
    }

    private int nextSortNo() {
        return newsRejectReasonMapper.selectAll().stream()
                .mapToInt(NewsRejectReasonPO::getSortNo)
                .max()
                .orElse(0) + 1;
    }
}
