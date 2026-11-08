package com.zengame.ai.business.news.channel.wechatmp.cover;

import com.zengame.ai.apiclient.wechatmp.WereadClient;
import com.zengame.ai.business.news.channel.bo.FetchedItem;
import com.zengame.ai.business.news.channel.bo.SourceCandidate;
import com.zengame.ai.business.news.channel.bo.SourceRecentItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

/**
 * 将微信读书返回的公众号和文章转为统一来源与资讯条目。
 */
@Mapper
public interface WechatMpConvert {

    WechatMpConvert INSTANCE = Mappers.getMapper(WechatMpConvert.class);

    @Mapping(target = "sourceKey", source = "bookId")
    @Mapping(target = "name", source = "book.title")
    @Mapping(target = "avatarUrl", source = "book.cover")
    @Mapping(target = "description", source = "book.intro")
    @Mapping(target = "handle", ignore = true)
    @Mapping(target = "stats", expression = "java(java.util.List.of())")
    SourceCandidate toSourceCandidate(String bookId, WereadClient.BookInfo book);

    @Mapping(target = "thumbnailUrl", source = "cover.pic")
    @Mapping(target = "publishTime", source = "published")
    @Mapping(target = "durationSeconds", ignore = true)
    @Mapping(target = "stats", expression = "java(java.util.List.of())")
    SourceRecentItem toRecentItem(WereadClient.Cover cover, LocalDateTime published);

    @Mapping(target = "canonicalUrl", source = "url")
    @Mapping(target = "bodyHtml", source = "html")
    @Mapping(target = "coverUrl", source = "cover.pic")
    @Mapping(target = "author", source = "cover.name")
    @Mapping(target = "authorAvatarUrl", source = "cover.avatar")
    @Mapping(target = "publishTime", source = "published")
    @Mapping(target = "feedTime", source = "published")
    @Mapping(target = "durationSeconds", ignore = true)
    @Mapping(target = "playback", ignore = true)
    @Mapping(target = "stats", expression = "java(java.util.List.of())")
    FetchedItem toFetchedItem(WereadClient.Cover cover, String url, String html, LocalDateTime published);
}
