package com.zengame.ai.business.news.feed.cover;

import com.zengame.ai.business.news.channel.bo.SourceCandidate;
import com.zengame.ai.business.news.channel.bo.SourcePreview;
import com.zengame.ai.business.news.channel.bo.SourceRecentItem;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.feed.domain.bo.FeedStateBO;
import com.zengame.ai.business.news.feed.domain.request.FeedCreateRequest;
import com.zengame.ai.business.news.feed.domain.vo.FeedVO;
import com.zengame.ai.business.news.feed.domain.vo.SourceCandidateVO;
import com.zengame.ai.business.news.feed.domain.vo.SourcePreviewVO;
import com.zengame.ai.business.news.feed.domain.vo.SourceRecentItemVO;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订阅源和来源搜索、预览结果的转换。
 */
@Mapper
public interface NewsFeedConvert {

    NewsFeedConvert INSTANCE = Mappers.getMapper(NewsFeedConvert.class);

    /**
     * 来源卡片「最近更新」的条数
     */
    int PREVIEW_RECENT_LIMIT = 3;

    /**
     * 订阅源转表格行。
     *
     * @param feed    订阅源
     * @param state   展示状态和失败原因
     * @param pulling 是否正在拉取
     * @return 订阅源表格行
     */
    @Mapping(target = "contentTypeDesc", source = "feed.contentType.desc")
    @Mapping(target = "status", source = "state.status")
    @Mapping(target = "statusDesc", source = "state.status.desc")
    @Mapping(target = "error", source = "state.error")
    @Mapping(target = "pulling", source = "pulling")
    FeedVO toVO(NewsFeedPO feed, FeedStateBO state, boolean pulling);

    /**
     * 订阅请求转新订阅源，拉取状态取库表默认值（待首次拉取）。
     *
     * @param channelCode 渠道标识
     * @param request     订阅请求
     * @return 新订阅源
     */
    @Mapping(target = "channelCode", source = "channelCode")
    @Mapping(target = "contentType", source = "request.contentType", qualifiedByName = "toContentType")
    NewsFeedPO toPO(String channelCode, FeedCreateRequest request);

    /**
     * @param candidates 插件返回的搜索结果
     * @return 搜索结果
     */
    List<SourceCandidateVO> toCandidateVOs(List<SourceCandidate> candidates);

    /**
     * @param preview 插件返回的来源预览
     * @return 来源卡片
     */
    @Mapping(target = "contentTypeDesc", source = "contentType.desc")
    @Mapping(target = "recentItems", source = "recentItems", qualifiedByName = "toLatestRecentItems")
    SourcePreviewVO toPreviewVO(SourcePreview preview);

    /**
     * @param items 插件返回的最近更新
     * @return 最近更新条目
     */
    List<SourceRecentItemVO> toRecentItemVOs(List<SourceRecentItem> items);

    /**
     * 来源卡片只展示发布时间最近的 3 条更新（插件返回的顺序可能含置顶内容）。
     *
     * @param items 插件返回的最近更新
     * @return 按发布时间倒序的前 3 条
     */
    @Named("toLatestRecentItems")
    default List<SourceRecentItemVO> toLatestRecentItems(List<SourceRecentItem> items) {
        return toRecentItemVOs(items.stream()
                .sorted(Comparator.comparing(SourceRecentItem::getPublishTime).reversed())
                .limit(PREVIEW_RECENT_LIMIT)
                .collect(Collectors.toList()));
    }

    /**
     * @param code 内容类型编码
     * @return 内容类型
     */
    @Named("toContentType")
    default NewsContentType toContentType(String code) {
        return NewsContentType.fromCode(code);
    }
}
