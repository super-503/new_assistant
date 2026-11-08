package com.zengame.ai.business.news.channel.cover;

import com.zengame.ai.business.news.channel.bo.FetchedItem;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Optional;

/**
 * 统一条目入库为资讯：订阅源决定归属和内容类型，条目提供原文信息，媒体地址取渠道原始地址。
 */
@Mapper
public interface NewsItemIngestConvert {

    NewsItemIngestConvert INSTANCE = Mappers.getMapper(NewsItemIngestConvert.class);

    /**
     * @param feed     来源订阅源
     * @param item     插件输出的统一条目
     * @param bodyHtml 清洗后的正文；正文不可获取时为空
     * @return 状态为加工中的新资讯
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "feedId", source = "feed.id")
    @Mapping(target = "channelCode", source = "feed.channelCode")
    @Mapping(target = "contentType", source = "feed.contentType")
    @Mapping(target = "canonicalUrl", source = "item.canonicalUrl")
    @Mapping(target = "originTitle", source = "item.title")
    @Mapping(target = "author", source = "item.author")
    @Mapping(target = "authorAvatarUrl", source = "item.authorAvatarUrl")
    @Mapping(target = "publishTime", source = "item.publishTime")
    @Mapping(target = "durationSeconds", source = "item.durationSeconds")
    @Mapping(target = "statsJson", source = "item.stats", qualifiedByName = "toJson")
    @Mapping(target = "coverUrl", source = "item.coverUrl")
    @Mapping(target = "bodyHtml", source = "bodyHtml")
    @Mapping(target = "playbackJson", source = "item.playback", qualifiedByName = "toJson")
    @Mapping(target = "status", constant = "PROCESSING")
    @Mapping(target = "aiValuesJson", ignore = true)
    @Mapping(target = "aiFailedKeysJson", ignore = true)
    @Mapping(target = "editedKeysJson", ignore = true)
    @Mapping(target = "rejectReasonId", ignore = true)
    @Mapping(target = "rejectNote", ignore = true)
    @Mapping(target = "reviewer", ignore = true)
    @Mapping(target = "reviewTime", ignore = true)
    @Mapping(target = "digestSentTime", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    NewsItemPO toItemPO(NewsFeedPO feed, FetchedItem item, String bodyHtml);

    /**
     * @param value 播放方式或统计项
     * @return JSON；值为空时为空
     */
    @Named("toJson")
    default String toJson(Object value) {
        return Optional.ofNullable(value).map(JacksonUtil::writeValueAsString).orElse(null);
    }
}
