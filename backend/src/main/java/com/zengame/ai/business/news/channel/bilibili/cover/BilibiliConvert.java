package com.zengame.ai.business.news.channel.bilibili.cover;

import com.zengame.ai.apiclient.bilibili.vo.BilibiliSearchUser;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliUserCard;
import com.zengame.ai.apiclient.bilibili.vo.BilibiliVideo;
import com.zengame.ai.business.news.channel.bo.FetchedItem;
import com.zengame.ai.business.news.channel.bo.Playback;
import com.zengame.ai.business.news.channel.bo.SourceCandidate;
import com.zengame.ai.business.news.channel.bo.SourceRecentItem;
import com.zengame.ai.business.news.channel.bo.StatItem;
import com.zengame.ai.business.news.domain.enums.NewsPlaybackKind;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * B站接口数据到渠道统一结构的转换：搜索结果、来源卡片、最近更新和统一条目。
 */
@Mapper
public interface BilibiliConvert {

    BilibiliConvert INSTANCE = Mappers.getMapper(BilibiliConvert.class);

    /**
     * @param card 用户卡片
     * @return 来源信息：标识为 mid，handle 为「UID:mid」，统计项为粉丝数
     */
    @Mapping(target = "sourceKey", source = "mid")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "handle", source = "mid", qualifiedByName = "uidHandle")
    @Mapping(target = "avatarUrl", source = "face", qualifiedByName = "httpsUrl")
    @Mapping(target = "description", source = "sign")
    @Mapping(target = "stats", source = "fans", qualifiedByName = "fanStats")
    SourceCandidate toCandidate(BilibiliUserCard card);

    /**
     * @param user 用户搜索结果
     * @return 来源信息：标识为 mid，handle 为「UID:mid」，统计项为粉丝数
     */
    @Mapping(target = "sourceKey", source = "mid")
    @Mapping(target = "name", source = "uname")
    @Mapping(target = "handle", source = "mid", qualifiedByName = "uidHandle")
    @Mapping(target = "avatarUrl", source = "upic", qualifiedByName = "httpsUrl")
    @Mapping(target = "description", source = "usign")
    @Mapping(target = "stats", source = "fans", qualifiedByName = "fanStats")
    SourceCandidate toCandidate(BilibiliSearchUser user);

    /**
     * @param video 投稿
     * @return 来源预览里的一条最近更新，统计项为播放数和评论数
     */
    @Mapping(target = "title", source = "title")
    @Mapping(target = "thumbnailUrl", source = "pic", qualifiedByName = "httpsUrl")
    @Mapping(target = "publishTime", source = "created", qualifiedByName = "toDateTime")
    @Mapping(target = "durationSeconds", source = "length", qualifiedByName = "parseDurationSeconds")
    @Mapping(target = "stats", expression = "java(videoStats(video))")
    SourceRecentItem toRecentItem(BilibiliVideo video);

    /**
     * @param video  投稿
     * @param author 投稿所属 UP 主
     * @return 统一条目：原文链接为视频页，正文为简介，播放方式为官方内嵌播放器
     */
    @Mapping(target = "canonicalUrl", source = "video.bvid", qualifiedByName = "videoUrl")
    @Mapping(target = "title", source = "video.title")
    @Mapping(target = "bodyHtml", source = "video.description", qualifiedByName = "descriptionToHtml")
    @Mapping(target = "coverUrl", source = "video.pic", qualifiedByName = "httpsUrl")
    @Mapping(target = "author", source = "author.name")
    @Mapping(target = "authorAvatarUrl", source = "author.face", qualifiedByName = "httpsUrl")
    @Mapping(target = "publishTime", source = "video.created", qualifiedByName = "toDateTime")
    @Mapping(target = "feedTime", source = "video.created", qualifiedByName = "toDateTime")
    @Mapping(target = "durationSeconds", source = "video.length", qualifiedByName = "parseDurationSeconds")
    @Mapping(target = "playback", source = "video.bvid", qualifiedByName = "embedPlayback")
    @Mapping(target = "stats", expression = "java(videoStats(video))")
    FetchedItem toFetchedItem(BilibiliVideo video, BilibiliUserCard author);

    /**
     * @param mid 用户 ID
     * @return 来源标识的展示形式，如「UID:2267573」
     */
    @Named("uidHandle")
    default String uidHandle(String mid) {
        return "UID:" + mid;
    }

    /**
     * @param url B站图片地址，可能是 http 或协议相对地址
     * @return https 地址；原值为空时为空
     */
    @Named("httpsUrl")
    default String httpsUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        if (url.startsWith("http://")) {
            return "https://" + url.substring("http://".length());
        }
        return url;
    }

    /**
     * @param fans 粉丝数
     * @return 统计项「粉丝」；粉丝数为空时为空列表
     */
    @Named("fanStats")
    default List<StatItem> fanStats(Long fans) {
        return Optional.ofNullable(fans)
                .map(value -> Collections.singletonList(new StatItem("粉丝", value)))
                .orElse(Collections.emptyList());
    }

    /**
     * @param video 投稿
     * @return 统计项「播放」「评论」；播放数被隐藏时不含播放
     */
    default List<StatItem> videoStats(BilibiliVideo video) {
        List<StatItem> stats = new ArrayList<>();
        Optional.ofNullable(video.getPlay())
                .filter(NumberUtils::isDigits)
                .ifPresent(play -> stats.add(new StatItem("播放", Long.valueOf(play))));
        Optional.ofNullable(video.getComment())
                .ifPresent(comment -> stats.add(new StatItem("评论", comment)));
        return stats;
    }

    /**
     * @param epochSecond 秒级时间戳
     * @return 系统时区的时间
     */
    @Named("toDateTime")
    default LocalDateTime toDateTime(Long epochSecond) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneId.systemDefault());
    }

    /**
     * @param length 时长，mm:ss 或 h:mm:ss，分钟数可以超过 59
     * @return 时长（秒）；投稿未提供时为空
     * @throws NumberFormatException 时长格式不正确
     */
    @Named("parseDurationSeconds")
    default Integer parseDurationSeconds(String length) {
        if (StringUtils.isBlank(length)) {
            return null;
        }
        int seconds = 0;
        for (String part : StringUtils.split(length, ':')) {
            seconds = seconds * 60 + Integer.parseInt(part.trim());
        }
        return seconds;
    }

    /**
     * @param description 视频简介，纯文本
     * @return HTML 转义后按换行分成的段落；简介为空时为空
     */
    @Named("descriptionToHtml")
    default String descriptionToHtml(String description) {
        if (StringUtils.isBlank(description)) {
            return null;
        }
        return Arrays.stream(description.split("\\r?\\n"))
                .filter(StringUtils::isNotBlank)
                .map(line -> "<p>" + HtmlUtils.htmlEscape(line) + "</p>")
                .collect(Collectors.joining());
    }

    /**
     * @param bvid 视频 BV 号
     * @return 视频页地址
     */
    @Named("videoUrl")
    default String videoUrl(String bvid) {
        return "https://www.bilibili.com/video/" + bvid;
    }

    /**
     * @param bvid 视频 BV 号
     * @return 官方内嵌播放器，不自动播放
     */
    @Named("embedPlayback")
    default Playback embedPlayback(String bvid) {
        return Playback.builder()
                .kind(NewsPlaybackKind.EMBED)
                .url("https://player.bilibili.com/player.html?bvid=" + bvid + "&autoplay=0")
                .build();
    }
}
