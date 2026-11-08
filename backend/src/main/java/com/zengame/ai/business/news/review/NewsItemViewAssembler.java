package com.zengame.ai.business.news.review;

import com.github.pagehelper.PageInfo;
import com.zengame.ai.business.news.NewsAiFieldCatalog;
import com.zengame.ai.business.news.review.cover.NewsItemConvert;
import com.zengame.ai.business.news.review.cover.NewsItemViewConvert;
import com.zengame.ai.business.news.review.domain.bo.NewsItemBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemQueryBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemViewContext;
import com.zengame.ai.business.news.review.domain.bo.NewsTagFilterBO;
import com.zengame.ai.business.news.review.domain.vo.NewsItemCardVO;
import com.zengame.ai.business.news.review.domain.vo.NewsItemDetailVO;
import com.zengame.ai.business.news.setting.NewsAiModelSettingService;
import com.zengame.ai.infrastructure.common.BasePageInfo;
import com.zengame.ai.infrastructure.common.PageHelperUtil;
import com.zengame.ai.infrastructure.common.ResultList;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.NewsAiFieldMapper;
import com.zengame.ai.repository.ai.mapper.NewsChannelMapper;
import com.zengame.ai.repository.ai.mapper.NewsFeedMapper;
import com.zengame.ai.repository.ai.mapper.NewsItemLogMapper;
import com.zengame.ai.repository.ai.mapper.NewsItemMapper;
import com.zengame.ai.repository.ai.mapper.NewsRejectReasonMapper;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import com.zengame.ai.repository.ai.po.NewsChannelPO;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资讯的读取与展示组装，审核和资讯库共用：加载配置快照，查询列表，组装卡片和详情。
 */
@Component
@RequiredArgsConstructor
public class NewsItemViewAssembler {

    private final NewsItemMapper newsItemMapper;

    private final NewsItemLogMapper newsItemLogMapper;

    private final NewsAiFieldMapper newsAiFieldMapper;

    private final NewsAiFieldCatalog newsAiFieldCatalog;

    private final NewsAiModelSettingService newsAiModelSettingService;

    private final NewsChannelMapper newsChannelMapper;

    private final NewsFeedMapper newsFeedMapper;

    private final NewsRejectReasonMapper newsRejectReasonMapper;

    /**
     * @return AI 字段、渠道、订阅源、拒绝理由的当前配置
     */
    public NewsItemViewContext loadContext() {
        List<NewsChannelPO> channels = newsChannelMapper.selectAll();
        List<NewsFeedPO> feeds = channels.stream()
                .flatMap(channel -> newsFeedMapper.selectByChannelCode(channel.getCode()).stream())
                .collect(Collectors.toList());
        return new NewsItemViewContext(newsAiFieldMapper.selectAll(), newsAiFieldCatalog::isAvailable,
                newsAiModelSettingService.isConfigured(), channels, feeds, newsRejectReasonMapper.selectAll());
    }

    /**
     * @param id 资讯主键
     * @return 资讯
     * @throws BusinessException 资讯不存在
     */
    public NewsItemBO load(Long id) {
        return find(id).orElseThrow(() -> new BusinessException("资讯不存在: " + id));
    }

    /**
     * @param id 资讯主键
     * @return 资讯；不存在时为空
     */
    public Optional<NewsItemBO> find(Long id) {
        return Optional.ofNullable(newsItemMapper.selectById(id)).map(NewsItemConvert.INSTANCE::toBO);
    }

    /** @return 用于审核结果的标题；资讯不存在时为空 */
    public Optional<String> findTitle(Long id) {
        return find(id).map(item -> item.displayTitle(newsAiModelSettingService.isConfigured()));
    }

    /**
     * 按条件分页查询资讯并组装为列表项。
     *
     * @param query    查询条件
     * @param pageInfo 分页参数
     * @param context  配置快照
     * @return 一页列表项
     * @throws BusinessException 筛选的字段不是启用中的标签字段
     */
    public ResultList<NewsItemCardVO> page(NewsItemQueryBO query, BasePageInfo pageInfo, NewsItemViewContext context) {
        checkTagFilters(query, context);
        query.setUseAiTitle(context.isModelConfigured());
        PageHelperUtil.startPage(pageInfo);
        PageInfo<NewsItemPO> page = new PageInfo<>(newsItemMapper.selectByQuery(query));
        List<NewsItemCardVO> cards = page.getList().stream()
                .map(NewsItemConvert.INSTANCE::toBO)
                .map(item -> NewsItemViewConvert.INSTANCE.toCard(item, context))
                .collect(Collectors.toList());
        return new ResultList<>(cards, new BasePageInfo(page.getPageNum(), page.getPageSize(), page.getTotal()));
    }

    /**
     * 组装资讯详情：资讯本身、AI 字段、处理记录。
     *
     * @param item 资讯
     * @return 详情
     */
    public NewsItemDetailVO detail(NewsItemBO item) {
        return NewsItemViewConvert.INSTANCE.toDetail(item, newsItemLogMapper.selectByItemId(item.getId()), loadContext());
    }

    /**
     * 标签筛选只接受启用中的标签字段。
     */
    private void checkTagFilters(NewsItemQueryBO query, NewsItemViewContext context) {
        Set<String> tagKeys = context.tagFields().stream().map(NewsAiFieldPO::getFieldKey).collect(Collectors.toSet());
        Optional.ofNullable(query.getTagFilters()).orElse(List.of()).stream()
                .map(NewsTagFilterBO::getFieldKey)
                .filter(fieldKey -> !tagKeys.contains(fieldKey))
                .findFirst()
                .ifPresent(fieldKey -> {
                    throw new BusinessException("不是可筛选的标签字段: " + fieldKey);
                });
    }
}
