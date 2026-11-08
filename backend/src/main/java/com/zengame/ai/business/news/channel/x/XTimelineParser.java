package com.zengame.ai.business.news.channel.x;

import com.fasterxml.jackson.databind.JsonNode;
import com.zengame.ai.business.news.channel.x.bo.XMedia;
import com.zengame.ai.business.news.channel.x.bo.XMediaType;
import com.zengame.ai.business.news.channel.x.bo.XPost;
import com.zengame.ai.business.news.channel.x.bo.XTimelineTweet;
import com.zengame.ai.business.news.channel.x.bo.XUrlEntity;
import com.zengame.ai.business.news.channel.x.bo.XUserProfile;
import com.zengame.ai.infrastructure.exception.BusinessException;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 解析 X 网页端 GraphQL 响应：用户资料（UserByScreenName / UserByRestId）和用户时间线（UserTweets）。
 *
 * <p>解析规则参照 RSSHub 的 {@code gatherLegacyFromData}：时间线取置顶、普通条目和自己的回复串，
 * 只保留订阅账号自己发的推文；转推带上被转的原帖，长推文取 note_tweet 的完整正文，引用帖解析一层。
 * 已删除、受限等没有正文的推文（Tombstone 等）不在结果里。
 */
@Component
public class XTimelineParser {

    private static final DateTimeFormatter CREATED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);

    private static final Pattern MEDIA_IMAGE_URL =
            Pattern.compile("^(https?://\\w+\\.twimg\\.com/media/[^/:?]+)\\.(jpg|jpeg|gif|png|bmp|webp)(:\\w+)?$", Pattern.CASE_INSENSITIVE);

    private static final String VISIBILITY_WRAPPER = "TweetWithVisibilityResults";

    /**
     * 解析用户资料。
     *
     * @param response UserByScreenName / UserByRestId 的响应
     * @return 用户资料；用户不存在或不可用（如已封禁）时为空
     */
    public Optional<XUserProfile> parseUser(JsonNode response) {
        return toUserProfile(response.at("/data/user/result"));
    }

    /**
     * 解析搜索框用户联想结果里的账号 id。
     *
     * @param response typeahead 的响应
     * @return 联想顺序的 rest_id；没有匹配时为空列表
     */
    public List<String> parseTypeaheadUserIds(JsonNode response) {
        return stream(required(response, "/users"))
                .map(user -> requiredText(user, "/id_str"))
                .collect(Collectors.toList());
    }

    /**
     * @param result 用户结果
     * @return 用户资料；不是可用用户（如 UserUnavailable）时为空
     */
    private Optional<XUserProfile> toUserProfile(JsonNode result) {
        return Optional.of(result)
                .filter(user -> "User".equals(user.path("__typename").asText()))
                .map(user -> XUserProfile.builder()
                        .restId(requiredText(user, "/rest_id"))
                        .name(requiredText(user, "/core/name"))
                        .screenName(requiredText(user, "/core/screen_name"))
                        .avatarUrl(toOriginalAvatarUrl(requiredText(user, "/avatar/image_url")))
                        .description(expandDescription(user.path("profile_bio")))
                        .followersCount(required(user, "/relationship_counts/followers").asLong())
                        .build());
    }

    /**
     * 解析当前登录账号的用户名。
     *
     * @param response Viewer 的响应
     * @return 不带 @ 的用户名
     * @throws IllegalStateException 响应里没有登录账号的 core.screen_name
     */
    public String parseViewerScreenName(JsonNode response) {
        return requiredText(response, "/data/viewer/user_results/result/core/screen_name");
    }

    /**
     * 解析用户时间线第一页里订阅账号自己的推文。
     *
     * @param response UserTweets 的响应
     * @param userId   订阅账号的 rest_id
     * @return 时间线顺序的推文，按 rest_id 去重
     * @throws BusinessException 响应里没有该用户的时间线
     */
    public List<XTimelineTweet> parseUserTweets(JsonNode response, String userId) {
        JsonNode userResult = response.at("/data/user/result");
        if (userResult.isMissingNode()) {
            throw new BusinessException("X 未返回用户时间线: " + userId);
        }
        Map<String, XTimelineTweet> tweetsById = new LinkedHashMap<>();
        collectTweetResults(required(userResult, "/timeline/timeline/instructions")).stream()
                .map(this::unwrap)
                .filter(result -> result.has("legacy"))
                .filter(result -> userId.equals(requiredText(result, "/legacy/user_id_str")))
                .map(this::toTimelineTweet)
                .flatMap(Optional::stream)
                .forEach(tweet -> tweetsById.putIfAbsent(tweet.getTweet().getRestId(), tweet));
        return new ArrayList<>(tweetsById.values());
    }

    /**
     * 从时间线指令里取出所有推文结果：置顶条目、普通条目、回复串模块里的条目。
     */
    private List<JsonNode> collectTweetResults(JsonNode instructions) {
        List<JsonNode> itemContents = new ArrayList<>();
        for (JsonNode instruction : instructions) {
            switch (instruction.path("type").asText()) {
                case "TimelinePinEntry":
                    collectEntry(required(instruction, "/entry"), itemContents);
                    break;
                case "TimelineAddEntries":
                    required(instruction, "/entries").forEach(entry -> collectEntry(entry, itemContents));
                    break;
                case "TimelineAddToModule":
                    required(instruction, "/moduleItems").forEach(item -> itemContents.add(item.at("/item/itemContent")));
                    break;
                default:
                    break;
            }
        }
        return itemContents.stream()
                .filter(itemContent -> itemContent.has("tweet_results"))
                .map(itemContent -> itemContent.at("/tweet_results/result"))
                .filter(result -> !result.isMissingNode())
                .collect(Collectors.toList());
    }

    /**
     * 单条目取 itemContent，模块条目取其中每一项的 itemContent。
     */
    private void collectEntry(JsonNode entry, List<JsonNode> itemContents) {
        JsonNode content = entry.path("content");
        itemContents.add(content.path("itemContent"));
        content.path("items").forEach(item -> itemContents.add(item.at("/item/itemContent")));
    }

    /**
     * 组装时间线条目；被转的原帖没有正文（已删除等）时不收录该转推。
     */
    private Optional<XTimelineTweet> toTimelineTweet(JsonNode result) {
        JsonNode retweetResult = result.at("/legacy/retweeted_status_result/result");
        if (retweetResult.isMissingNode()) {
            return Optional.of(XTimelineTweet.builder()
                    .tweet(toPost(result, true))
                    .retweeted(Optional.empty())
                    .build());
        }
        return Optional.of(unwrap(retweetResult))
                .filter(retweeted -> retweeted.has("legacy"))
                .map(retweeted -> XTimelineTweet.builder()
                        .tweet(toPost(result, false))
                        .retweeted(Optional.of(toPost(retweeted, true)))
                        .build());
    }

    /**
     * 解析一条推文。
     *
     * @param result    推文结果（已去掉可见性包装）
     * @param withQuote 是否解析引用帖
     */
    private XPost toPost(JsonNode result, boolean withQuote) {
        JsonNode legacy = required(result, "/legacy");
        JsonNode user = required(result, "/core/user_results/result");
        JsonNode noteTweet = result.at("/note_tweet/note_tweet_results/result");
        boolean longform = !noteTweet.isMissingNode();
        String text = longform ? requiredText(noteTweet, "/text") : requiredText(legacy, "/full_text");
        JsonNode entities = longform ? required(noteTweet, "/entity_set") : required(legacy, "/entities");
        Optional<XPost> quoted = Optional.empty();
        if (withQuote) {
            quoted = Optional.of(unwrap(result.at("/quoted_status_result/result")))
                    .filter(quote -> quote.has("legacy"))
                    .map(quote -> toPost(quote, false));
        }
        return XPost.builder()
                .restId(requiredText(result, "/rest_id"))
                .authorName(requiredText(user, "/core/name"))
                .authorScreenName(requiredText(user, "/core/screen_name"))
                .authorAvatarUrl(toOriginalAvatarUrl(requiredText(user, "/avatar/image_url")))
                .createdAt(parseCreatedAt(requiredText(legacy, "/created_at")))
                .text(Parser.unescapeEntities(text, false))
                .urls(stream(entities.path("urls"))
                        .filter(url -> url.hasNonNull("expanded_url"))
                        .map(url -> XUrlEntity.builder()
                                .url(requiredText(url, "/url"))
                                .expandedUrl(requiredText(url, "/expanded_url"))
                                .build())
                        .collect(Collectors.toList()))
                .mentions(stream(entities.path("user_mentions"))
                        .map(mention -> requiredText(mention, "/screen_name"))
                        .collect(Collectors.toList()))
                .hashtags(stream(entities.path("hashtags"))
                        .map(hashtag -> requiredText(hashtag, "/text"))
                        .collect(Collectors.toList()))
                .media(stream(legacy.at("/extended_entities/media"))
                        .map(this::toMedia)
                        .collect(Collectors.toList()))
                .quoted(quoted)
                .favoriteCount(required(legacy, "/favorite_count").asLong())
                .replyCount(required(legacy, "/reply_count").asLong())
                .build();
    }

    /**
     * 解析一个媒体：图片取原图，视频和 GIF 取码率最高的 mp4 和封面。
     */
    private XMedia toMedia(JsonNode media) {
        XMediaType type = XMediaType.fromCode(requiredText(media, "/type"));
        String mediaUrl = requiredText(media, "/media_url_https");
        XMedia.XMediaBuilder builder = XMedia.builder()
                .type(type)
                .tcoUrl(requiredText(media, "/url"));
        if (type == XMediaType.PHOTO) {
            return builder.imageUrl(toOriginalImageUrl(mediaUrl))
                    .videoUrl(Optional.empty())
                    .durationSeconds(Optional.empty())
                    .build();
        }
        JsonNode videoInfo = required(media, "/video_info");
        Optional<String> bestMp4 = stream(required(videoInfo, "/variants"))
                .filter(variant -> "video/mp4".equals(variant.path("content_type").asText()))
                .max(Comparator.comparingLong(variant -> variant.path("bitrate").asLong()))
                .map(variant -> requiredText(variant, "/url"));
        Optional<Integer> durationSeconds = Optional.of(videoInfo.path("duration_millis"))
                .filter(JsonNode::isNumber)
                .map(millis -> (int) Math.round(millis.asLong() / 1000.0));
        return builder.imageUrl(toOriginalImageUrl(mediaUrl))
                .videoUrl(bestMp4)
                .durationSeconds(durationSeconds)
                .build();
    }

    /**
     * 去掉可见性包装（TweetWithVisibilityResults），得到推文本体。
     */
    private JsonNode unwrap(JsonNode result) {
        return VISIBILITY_WRAPPER.equals(result.path("__typename").asText()) ? required(result, "/tweet") : result;
    }

    /**
     * 简介里的 t.co 短链换成原始链接。
     */
    private String expandDescription(JsonNode profileBio) {
        return Optional.of(profileBio.path("description"))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .map(description -> stream(profileBio.at("/entities/description/urls"))
                        .filter(url -> url.hasNonNull("expanded_url"))
                        .reduce(description,
                                (text, url) -> text.replace(requiredText(url, "/url"), requiredText(url, "/expanded_url")),
                                (left, right) -> right))
                .map(description -> Parser.unescapeEntities(description, false))
                .orElse(null);
    }

    /**
     * pbs.twimg.com/media 图片换成原图地址，如 .../media/abc.jpg → .../media/abc?format=jpg&name=orig。
     */
    static String toOriginalImageUrl(String url) {
        Matcher matcher = MEDIA_IMAGE_URL.matcher(url);
        if (!matcher.matches()) {
            return url;
        }
        String format = matcher.group(2).equalsIgnoreCase("jpeg") ? "jpg" : matcher.group(2).toLowerCase(Locale.ROOT);
        return matcher.group(1) + "?format=" + format + "&name=orig";
    }

    /**
     * 头像地址去掉 _normal 尺寸后缀，得到原尺寸头像。
     */
    private static String toOriginalAvatarUrl(String url) {
        return url.replace("_normal.", ".");
    }

    private static LocalDateTime parseCreatedAt(String createdAt) {
        return ZonedDateTime.parse(createdAt, CREATED_AT_FORMATTER)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private static Stream<JsonNode> stream(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false);
    }

    /**
     * @throws IllegalStateException 响应缺少该字段
     */
    private static JsonNode required(JsonNode node, String pointer) {
        JsonNode value = node.at(pointer);
        if (value.isMissingNode() || value.isNull()) {
            throw new IllegalStateException("X 响应缺少字段: " + pointer);
        }
        return value;
    }

    private static String requiredText(JsonNode node, String pointer) {
        return required(node, pointer).asText();
    }
}
