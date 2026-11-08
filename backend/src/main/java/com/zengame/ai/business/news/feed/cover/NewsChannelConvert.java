package com.zengame.ai.business.news.feed.cover;

import com.zengame.ai.business.news.channel.bo.ChannelDescriptor;
import com.zengame.ai.business.news.feed.domain.vo.ChannelRunVO;
import com.zengame.ai.business.news.feed.domain.vo.ChannelTodoVO;
import com.zengame.ai.business.news.feed.domain.vo.ChannelVO;
import com.zengame.ai.business.news.feed.domain.vo.FeedVO;
import com.zengame.ai.business.news.feed.domain.vo.TodoFeedVO;
import com.zengame.ai.repository.ai.po.NewsChannelPO;
import com.zengame.ai.repository.ai.po.NewsChannelRunPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 渠道和运行记录的转换。
 */
@Mapper
public interface NewsChannelConvert {

    NewsChannelConvert INSTANCE = Mappers.getMapper(NewsChannelConvert.class);

    /**
     * 渠道记录加插件描述转渠道视图：标识、名称、能力取自插件描述，启用和凭证取自渠道记录。
     *
     * @param channel        渠道记录
     * @param descriptor     插件描述
     * @param feedCount      订阅源数量
     * @param needsAttention 是否需要处理
     * @return 渠道视图
     */
    @Mapping(target = "code", source = "descriptor.code")
    @Mapping(target = "name", source = "descriptor.name")
    @Mapping(target = "domain", source = "descriptor.domain")
    @Mapping(target = "credentialMode", source = "descriptor.credentialMode")
    @Mapping(target = "searchable", source = "descriptor.searchable")
    @Mapping(target = "searchHint", source = "descriptor.searchHint")
    @Mapping(target = "defaultContentType", source = "descriptor.defaultContentType")
    @Mapping(target = "enabled", source = "channel.enabled")
    @Mapping(target = "credentialStatus", source = "channel.credentialStatus")
    @Mapping(target = "credentialStatusDesc", source = "channel.credentialStatus.desc")
    @Mapping(target = "credentialAccount", source = "channel.credentialAccount")
    @Mapping(target = "credentialUpdateTime", source = "channel.credentialUpdateTime")
    @Mapping(target = "feedCount", source = "feedCount")
    @Mapping(target = "needsAttention", source = "needsAttention")
    ChannelVO toVO(NewsChannelPO channel, ChannelDescriptor descriptor, int feedCount, boolean needsAttention);

    /**
     * @param run      运行记录
     * @param feedName 手动触发时拉取的订阅源显示名称；没有时为 null
     * @return 运行记录视图
     */
    @Mapping(target = "triggerTypeDesc", source = "run.triggerType.desc")
    @Mapping(target = "statusDesc", source = "run.status.desc")
    @Mapping(target = "feedName", source = "feedName")
    ChannelRunVO toRunVO(NewsChannelRunPO run, String feedName);

    /**
     * @param channel 订阅源所属渠道
     * @param feed    拉取失败的订阅源
     * @return 「需处理」里的一个订阅源
     */
    @Mapping(target = "channelCode", source = "channel.code")
    @Mapping(target = "channelName", source = "channel.name")
    @Mapping(target = "feed", source = "feed")
    TodoFeedVO toTodoFeedVO(ChannelVO channel, FeedVO feed);

    /**
     * @param credentialChannels 凭证已失效的渠道
     * @param failedFeeds        拉取失败的订阅源
     * @return 「需处理」汇总，总数为两部分之和
     */
    @Mapping(target = "total", expression = "java(credentialChannels.size() + failedFeeds.size())")
    @Mapping(target = "credentialChannels", source = "credentialChannels")
    @Mapping(target = "failedFeeds", source = "failedFeeds")
    ChannelTodoVO toTodoVO(List<ChannelVO> credentialChannels, List<TodoFeedVO> failedFeeds);
}
