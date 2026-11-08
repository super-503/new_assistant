package com.zengame.ai.business.news.library;

import com.zengame.ai.business.news.NewsAiFieldCatalog;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import com.zengame.ai.business.news.library.cover.NewsLibraryConvert;
import com.zengame.ai.business.news.library.domain.bo.NewsItemGroupCountBO;
import com.zengame.ai.business.news.library.domain.request.NewsLibraryPageQueryRequest;
import com.zengame.ai.business.news.library.domain.vo.NewsLibraryChannelNodeVO;
import com.zengame.ai.business.news.library.domain.vo.NewsLibraryFeedNodeVO;
import com.zengame.ai.business.news.library.domain.vo.NewsLibraryFilterOptionsVO;
import com.zengame.ai.business.news.library.domain.vo.NewsLibraryTreeVO;
import com.zengame.ai.business.news.library.domain.vo.NewsLibraryTypeNodeVO;
import com.zengame.ai.business.news.review.NewsAiFieldRules;
import com.zengame.ai.business.news.review.NewsItemFieldEditor;
import com.zengame.ai.business.news.review.NewsItemViewAssembler;
import com.zengame.ai.business.news.review.cover.NewsItemConvert;
import com.zengame.ai.business.news.review.cover.NewsItemViewConvert;
import com.zengame.ai.business.news.review.domain.bo.NewsFieldEditBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemViewContext;
import com.zengame.ai.business.news.review.domain.request.NewsFieldSaveRequest;
import com.zengame.ai.business.news.review.domain.vo.NewsItemCardVO;
import com.zengame.ai.business.news.review.domain.vo.NewsItemDetailVO;
import com.zengame.ai.business.news.setting.NewsAiModelSettingService;
import com.zengame.ai.infrastructure.common.ResultList;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.NewsItemMapper;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import com.zengame.ai.repository.ai.po.NewsChannelPO;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资讯库：只看已通过的资讯。分类树、分页、详情、修改标签字段。
 */
@Service
@RequiredArgsConstructor
public class NewsLibraryService {

    private final NewsItemMapper newsItemMapper;

    private final NewsAiFieldCatalog newsAiFieldCatalog;

    private final NewsAiModelSettingService newsAiModelSettingService;

    private final NewsItemViewAssembler newsItemViewAssembler;

    private final NewsItemFieldEditor newsItemFieldEditor;

    /**
     * 分类树：文章、视频两类各带总数，类型下按渠道分组再到订阅源，每级带数量。
     * 已删除订阅源的资讯只计入渠道和类型总数，不出现在订阅源这一级。
     *
     * @return 分类树
     */
    public NewsLibraryTreeVO tree() {
        NewsItemViewContext context = newsItemViewAssembler.loadContext();
        List<NewsItemGroupCountBO> counts = newsItemMapper.selectGroupCountByStatus(NewsItemStatus.APPROVED);
        List<NewsLibraryTypeNodeVO> types = Arrays.stream(NewsContentType.values())
                .map(contentType -> typeNode(contentType, counts.stream()
                        .filter(count -> count.getContentType() == contentType)
                        .collect(Collectors.toList()), context))
                .collect(Collectors.toList());
        return new NewsLibraryTreeVO(types);
    }

    /**
     * 资讯库分页，按发布时间从新到旧。
     *
     * @param request 查询条件与分页参数
     * @return 一页列表项
     * @throws BusinessException 筛选的字段不是启用中的标签字段
     */
    public ResultList<NewsItemCardVO> page(NewsLibraryPageQueryRequest request) {
        return newsItemViewAssembler.page(NewsLibraryConvert.INSTANCE.toQuery(request.getQuery()),
                request.getBasePageInfo(), newsItemViewAssembler.loadContext());
    }

    /**
     * @return 筛选栏的标签字段
     */
    public NewsLibraryFilterOptionsVO filterOptions() {
        return new NewsLibraryFilterOptionsVO(
                NewsItemViewConvert.INSTANCE.toTagFields(newsItemViewAssembler.loadContext().tagFields()));
    }

    /**
     * @param id 资讯
     * @return 资讯详情
     * @throws BusinessException 资讯不存在或不在资讯库
     */
    public NewsItemDetailVO detail(Long id) {
        NewsItemBO item = newsItemViewAssembler.load(id);
        requireApproved(item);
        return newsItemViewAssembler.detail(item);
    }

    /**
     * 修改已通过资讯的标签字段：只能改适用于该资讯、启用且允许人工修改的标签字段（显示为标签的字段和岗位分类）。
     * 提交的必填字段不能为空。
     *
     * @param id       资讯
     * @param request  修改内容
     * @param operator 操作人登录名
     * @return 修改后的详情
     * @throws BusinessException 资讯不存在或不在资讯库；字段不允许修改、值不合法、没有变化；必填字段为空
     */
    @Transactional(rollbackFor = Exception.class)
    public NewsItemDetailVO updateTags(Long id, NewsFieldSaveRequest request, String operator) {
        newsAiModelSettingService.requireConfigured();
        NewsItemBO item = Optional.ofNullable(newsItemMapper.selectByIdForUpdate(id))
                .map(NewsItemConvert.INSTANCE::toBO)
                .orElseThrow(() -> new BusinessException("资讯不存在: " + id));
        requireApproved(item);
        List<NewsAiFieldPO> tagFields = newsAiFieldCatalog.listEnabled().stream()
                .filter(NewsAiFieldRules::isTagField)
                .filter(NewsAiFieldPO::getEditable)
                .filter(field -> NewsAiFieldRules.isApplicable(field, item.getContentType()))
                .collect(Collectors.toList());
        List<NewsFieldEditBO> edits = NewsItemConvert.INSTANCE.toEdits(request.getFields());
        List<NewsAiFieldPO> changed = newsItemFieldEditor.apply(item, tagFields, edits);
        Set<String> submittedKeys = edits.stream().map(NewsFieldEditBO::getFieldKey).collect(Collectors.toSet());
        newsItemFieldEditor.checkRequired(item, tagFields.stream()
                .filter(field -> submittedKeys.contains(field.getFieldKey()))
                .collect(Collectors.toList()));
        newsItemMapper.updateById(NewsItemConvert.INSTANCE.toPO(item));
        newsItemFieldEditor.recordEdited(id, changed, operator);
        return newsItemViewAssembler.detail(item);
    }

    /**
     * @throws BusinessException 资讯不是已通过
     */
    private void requireApproved(NewsItemBO item) {
        if (item.getStatus() != NewsItemStatus.APPROVED) {
            throw new BusinessException("资讯不在资讯库: " + item.getId());
        }
    }

    /**
     * 一个类型的节点：有资讯的渠道按渠道主键升序，渠道下列出有资讯的现存订阅源。
     */
    private NewsLibraryTypeNodeVO typeNode(NewsContentType contentType, List<NewsItemGroupCountBO> counts,
                                           NewsItemViewContext context) {
        Map<String, List<NewsItemGroupCountBO>> byChannel = counts.stream()
                .collect(Collectors.groupingBy(NewsItemGroupCountBO::getChannelCode));
        List<NewsLibraryChannelNodeVO> channels = context.getChannels().stream()
                .filter(channel -> byChannel.containsKey(channel.getCode()))
                .map(channel -> channelNode(channel, byChannel.get(channel.getCode()), context))
                .collect(Collectors.toList());
        return new NewsLibraryTypeNodeVO(contentType, contentType.getDesc(), sum(counts), channels);
    }

    private NewsLibraryChannelNodeVO channelNode(NewsChannelPO channel, List<NewsItemGroupCountBO> counts,
                                                 NewsItemViewContext context) {
        Map<Long, Long> countByFeed = counts.stream()
                .collect(Collectors.toMap(NewsItemGroupCountBO::getFeedId, NewsItemGroupCountBO::getItemCount));
        List<NewsLibraryFeedNodeVO> feeds = context.getFeeds().stream()
                .filter(feed -> feed.getChannelCode().equals(channel.getCode()))
                .filter(feed -> countByFeed.containsKey(feed.getId()))
                .map(feed -> feedNode(feed, countByFeed.get(feed.getId())))
                .collect(Collectors.toList());
        return new NewsLibraryChannelNodeVO(channel.getCode(), channel.getName(), sum(counts), feeds);
    }

    private NewsLibraryFeedNodeVO feedNode(NewsFeedPO feed, long count) {
        return new NewsLibraryFeedNodeVO(feed.getId(), feed.getDisplayName(), feed.getAvatarUrl(), count);
    }

    private long sum(List<NewsItemGroupCountBO> counts) {
        return counts.stream().mapToLong(NewsItemGroupCountBO::getItemCount).sum();
    }
}
