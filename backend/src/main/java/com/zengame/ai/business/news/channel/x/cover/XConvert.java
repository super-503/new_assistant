package com.zengame.ai.business.news.channel.x.cover;

import com.zengame.ai.business.news.channel.bo.FetchedItem;
import com.zengame.ai.business.news.channel.bo.Playback;
import com.zengame.ai.business.news.channel.bo.SourceCandidate;
import com.zengame.ai.business.news.channel.bo.SourceRecentItem;
import com.zengame.ai.business.news.channel.bo.StatItem;
import com.zengame.ai.business.news.channel.x.bo.XMedia;
import com.zengame.ai.business.news.channel.x.bo.XPost;
import com.zengame.ai.business.news.channel.x.bo.XTimelineTweet;
import com.zengame.ai.business.news.channel.x.bo.XUserProfile;
import com.zengame.ai.business.news.domain.enums.NewsPlaybackKind;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * X 插件的对象转换：用户资料 → 搜索结果，时间线推文 → 统一条目 / 最近更新，媒体文件 → 媒体内容。
 */
@Mapper
public interface XConvert {

    XConvert INSTANCE = Mappers.getMapper(XConvert.class);

    /**
     * @param profile 用户资料
     * @return 搜索结果，handle 为 @用户名，统计项为关注者数
     */
    @Mapping(target = "sourceKey", source = "restId")
    @Mapping(target = "handle", source = "screenName", qualifiedByName = "toHandle")
    @Mapping(target = "stats", source = "followersCount", qualifiedByName = "toFollowerStats")
    SourceCandidate toSourceCandidate(XUserProfile profile);

    /**
     * 原文链接、作者、发布时间、正文都取入库内容（转推为被转的原帖），订阅源出现时间取时间线上推文的发帖时间。
     * 标题为空，由 AI 字段生成。
     *
     * @param tweet    时间线推文
     * @param bodyHtml 拼装好的正文 HTML
     * @return 统一条目
     */
    @Mapping(target = "canonicalUrl", source = "tweet.content.permalink")
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "bodyHtml", source = "bodyHtml")
    @Mapping(target = "coverUrl", source = "tweet.content", qualifiedByName = "toCoverUrl")
    @Mapping(target = "author", source = "tweet.content.authorName")
    @Mapping(target = "authorAvatarUrl", source = "tweet.content.authorAvatarUrl")
    @Mapping(target = "publishTime", source = "tweet.content.createdAt")
    @Mapping(target = "feedTime", source = "tweet.tweet.createdAt")
    @Mapping(target = "durationSeconds", source = "tweet.content", qualifiedByName = "toDurationSeconds")
    @Mapping(target = "playback", source = "tweet.content", qualifiedByName = "toPlayback")
    @Mapping(target = "stats", source = "tweet.content", qualifiedByName = "toPostStats")
    FetchedItem toFetchedItem(XTimelineTweet tweet, String bodyHtml);

    /**
     * @param tweet 时间线推文
     * @param title 推文文本的开头
     * @return 来源预览的最近更新
     */
    @Mapping(target = "title", source = "title")
    @Mapping(target = "thumbnailUrl", source = "tweet.content", qualifiedByName = "toCoverUrl")
    @Mapping(target = "publishTime", source = "tweet.content.createdAt")
    @Mapping(target = "durationSeconds", source = "tweet.content", qualifiedByName = "toDurationSeconds")
    @Mapping(target = "stats", source = "tweet.content", qualifiedByName = "toPostStats")
    SourceRecentItem toRecentItem(XTimelineTweet tweet, String title);

    @Named("toHandle")
    default String toHandle(String screenName) {
        return "@" + screenName;
    }

    @Named("toFollowerStats")
    default List<StatItem> toFollowerStats(Long followersCount) {
        return Collections.singletonList(new StatItem("关注者", followersCount));
    }

    /**
     * 封面：第一张图片或第一个视频的封面；没有媒体时为空。
     */
    @Named("toCoverUrl")
    default String toCoverUrl(XPost post) {
        return post.getMedia().stream()
                .findFirst()
                .map(XMedia::getImageUrl)
                .orElse(null);
    }

    /**
     * 播放方式：第一个有 mp4 的视频或 GIF，FILE 类型，带封面；没有时为空。
     */
    @Named("toPlayback")
    default Playback toPlayback(XPost post) {
        return post.getMedia().stream()
                .filter(media -> media.getVideoUrl().isPresent())
                .findFirst()
                .map(media -> new Playback(NewsPlaybackKind.FILE, media.getVideoUrl().get(), media.getImageUrl()))
                .orElse(null);
    }

    /**
     * 视频时长：第一个带时长的视频；没有时为空。
     */
    @Named("toDurationSeconds")
    default Integer toDurationSeconds(XPost post) {
        return post.getMedia().stream()
                .filter(media -> media.getDurationSeconds().isPresent())
                .findFirst()
                .flatMap(XMedia::getDurationSeconds)
                .orElse(null);
    }

    @Named("toPostStats")
    default List<StatItem> toPostStats(XPost post) {
        return Arrays.asList(new StatItem("点赞", post.getFavoriteCount()), new StatItem("回复", post.getReplyCount()));
    }
}
