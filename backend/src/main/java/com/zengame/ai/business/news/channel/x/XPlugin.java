package com.zengame.ai.business.news.channel.x;

import com.fasterxml.jackson.databind.JsonNode;
import com.zengame.ai.apiclient.x.XClient;
import com.zengame.ai.business.news.channel.ChannelLoginExpiredException;
import com.zengame.ai.business.news.channel.ChannelPlugin;
import com.zengame.ai.business.news.channel.TokenCredential;
import com.zengame.ai.business.news.channel.bo.ChannelCredential;
import com.zengame.ai.business.news.channel.bo.ChannelDescriptor;
import com.zengame.ai.business.news.channel.bo.CredentialVerifyResult;
import com.zengame.ai.business.news.channel.bo.FeedCursor;
import com.zengame.ai.business.news.channel.bo.FetchResult;
import com.zengame.ai.business.news.channel.bo.FetchedItem;
import com.zengame.ai.business.news.channel.bo.SourceCandidate;
import com.zengame.ai.business.news.channel.bo.SourcePreview;
import com.zengame.ai.business.news.channel.bo.SourceRecentItem;
import com.zengame.ai.business.news.channel.x.bo.XCredential;
import com.zengame.ai.business.news.channel.x.bo.XTimelineTweet;
import com.zengame.ai.business.news.channel.x.bo.XUserProfile;
import com.zengame.ai.business.news.channel.x.cover.XConvert;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsCredentialMode;
import com.zengame.ai.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * X 渠道插件：订阅 X 用户，拉取其主页时间线（原创、回复、引用、转推）。
 *
 * <ul>
 *     <li>凭证：运营粘贴网页端 auth_token，ct0 由 {@link XClient} 运行时换取；</li>
 *     <li>搜索：按名称或用户名搜索账号，用户名完全一致的账号排第一；</li>
 *     <li>拉取：UserTweets 第一页，转推按被转的原帖入库，订阅源出现时间取转推时间；</li>
 *     <li>正文：推文文本、原图、视频（playback 为 FILE）、引用帖的引用块，由 {@link XPostRenderer} 拼装。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class XPlugin implements ChannelPlugin, TokenCredential {

    private static final ChannelDescriptor DESCRIPTOR = ChannelDescriptor.builder()
            .code("x")
            .name("X")
            .domain("x.com")
            .defaultContentType(NewsContentType.ARTICLE)
            .credentialMode(NewsCredentialMode.TOKEN)
            .searchable(true)
            .searchHint("输入名称或 @用户名搜索")
            .embedDomains(Collections.emptyList())
            .build();

    private static final int RECENT_TITLE_MAX_LENGTH = 80;

    /**
     * X 用户名格式：字母、数字、下划线，最长 15 位
     */
    private static final Pattern SCREEN_NAME = Pattern.compile("^\\w{1,15}$");

    private final XClient xClient;

    private final XTimelineParser timelineParser;

    private final XPostRenderer postRenderer;

    @Override
    public ChannelDescriptor descriptor() {
        return DESCRIPTOR;
    }

    /**
     * 把 auth_token 组装成 X 凭证。
     *
     * @param token 运营粘贴的 auth_token
     * @return 渠道凭证
     * @throws BusinessException auth_token 为空
     */
    @Override
    public ChannelCredential createCredential(String token) {
        if (StringUtils.isBlank(token)) {
            throw new BusinessException("auth_token 不能为空");
        }
        return ChannelCredential.from(new XCredential(token.trim()));
    }

    /**
     * 查询 auth_token 当前登录的账号。
     *
     * @param credential X 凭证
     * @return 可用时账号为 @用户名；X 返回 401 / 403、未登录错误或未下发 ct0 时不可用
     */
    @Override
    public CredentialVerifyResult verify(ChannelCredential credential) {
        JsonNode viewer;
        try {
            viewer = xClient.getViewer(readAuthToken(credential));
        } catch (ChannelLoginExpiredException e) {
            return CredentialVerifyResult.invalid(e.getMessage());
        }
        return CredentialVerifyResult.valid("@" + timelineParser.parseViewerScreenName(viewer));
    }

    /**
     * 按名称或用户名搜索账号，关键词开头的 @ 可省略。
     *
     * <p>候选账号取自网页端搜索框的用户联想，再逐个查询完整资料（简介、关注者数）。
     * 关键词与某个用户名相同（不区分大小写）时，该账号排第一：联想结果里有就提前，没有就按用户名精确查找后放在最前。
     *
     * @param keyword    名称或 @用户名
     * @param credential X 凭证
     * @return 搜索结果，按 rest_id 去重；关键词为空时为空列表
     * @throws ChannelLoginExpiredException auth_token 已失效
     */
    @Override
    public List<SourceCandidate> search(String keyword, ChannelCredential credential) {
        String query = StringUtils.removeStart(StringUtils.trimToEmpty(keyword), "@").trim();
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }
        String authToken = readAuthToken(credential);
        List<XUserProfile> people = searchPeople(authToken, query);
        Optional<XUserProfile> exactMatch = people.stream()
                .filter(profile -> profile.getScreenName().equalsIgnoreCase(query))
                .findFirst()
                .or(() -> findByScreenName(authToken, query));
        Map<String, XUserProfile> profilesById = new LinkedHashMap<>();
        exactMatch.ifPresent(profile -> profilesById.put(profile.getRestId(), profile));
        people.forEach(profile -> profilesById.putIfAbsent(profile.getRestId(), profile));
        return profilesById.values().stream()
                .map(XConvert.INSTANCE::toSourceCandidate)
                .collect(Collectors.toList());
    }

    /**
     * 用户资料加时间线第一页，最近更新按发布时间倒序。
     *
     * @param sourceKey  用户 rest_id
     * @param credential X 凭证
     * @return 来源卡片
     * @throws BusinessException            用户不存在或不可用
     * @throws ChannelLoginExpiredException auth_token 已失效
     */
    @Override
    public SourcePreview preview(String sourceKey, ChannelCredential credential) {
        String authToken = readAuthToken(credential);
        XUserProfile profile = timelineParser.parseUser(xClient.getUserByRestId(authToken, sourceKey))
                .orElseThrow(() -> new BusinessException("X 用户不存在或不可用: " + sourceKey));
        List<SourceRecentItem> recentItems = fetchTimeline(authToken, sourceKey).stream()
                .sorted(Comparator.comparing((XTimelineTweet tweet) -> tweet.getContent().getCreatedAt()).reversed())
                .map(tweet -> XConvert.INSTANCE.toRecentItem(tweet,
                        StringUtils.abbreviate(postRenderer.renderPlainText(tweet.getContent()), RECENT_TITLE_MAX_LENGTH)))
                .collect(Collectors.toList());
        return SourcePreview.builder()
                .source(XConvert.INSTANCE.toSourceCandidate(profile))
                .contentType(DESCRIPTOR.getDefaultContentType())
                .recentItems(recentItems)
                .build();
    }

    /**
     * 拉取时间线第一页里转推时间或发帖时间晚于拉取进度的推文。
     *
     * @param cursor     用户 rest_id 和拉取进度
     * @param credential X 凭证
     * @return 新推文的统一条目，按时间线顺序
     * @throws ChannelLoginExpiredException auth_token 已失效
     */
    @Override
    public FetchResult fetch(FeedCursor cursor, ChannelCredential credential) {
        List<FetchedItem> items = fetchTimeline(readAuthToken(credential), cursor.getSourceKey()).stream()
                .filter(tweet -> tweet.getTweet().getCreatedAt().isAfter(cursor.getSince()))
                .map(tweet -> XConvert.INSTANCE.toFetchedItem(tweet, postRenderer.renderBody(tweet.getContent())))
                .collect(Collectors.toList());
        return FetchResult.builder().items(items).build();
    }

    /**
     * 取用户时间线第一页，同一原帖（如多次转推）只保留时间线上最先出现的一条。
     */
    private List<XTimelineTweet> fetchTimeline(String authToken, String userId) {
        List<XTimelineTweet> tweets = timelineParser.parseUserTweets(xClient.getUserTweets(authToken, userId), userId);
        return new ArrayList<>(tweets.stream()
                .collect(Collectors.toMap(tweet -> tweet.getContent().getPermalink(), Function.identity(),
                        (first, second) -> first, LinkedHashMap::new))
                .values());
    }

    /**
     * 搜索框用户联想取候选账号，再逐个按 rest_id 查完整资料，保持联想顺序。
     *
     * @return 候选账号资料，不可用的账号不在结果里；没有联想结果时为空列表
     */
    private List<XUserProfile> searchPeople(String authToken, String keyword) {
        return timelineParser.parseTypeaheadUserIds(xClient.searchUsersTypeahead(authToken, keyword)).stream()
                .map(restId -> timelineParser.parseUser(xClient.getUserByRestId(authToken, restId)))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    /**
     * 关键词符合用户名格式时按用户名精确查找。
     *
     * @return 用户资料；关键词不是合法用户名或用户不存在时为空
     */
    private Optional<XUserProfile> findByScreenName(String authToken, String keyword) {
        if (!SCREEN_NAME.matcher(keyword).matches()) {
            return Optional.empty();
        }
        return timelineParser.parseUser(xClient.getUserByScreenName(authToken, keyword));
    }

    /**
     * @throws ChannelLoginExpiredException 渠道没有凭证
     */
    private String readAuthToken(ChannelCredential credential) {
        if (!credential.isPresent()) {
            throw new ChannelLoginExpiredException("X 渠道未配置 auth_token");
        }
        return credential.read(XCredential.class).getAuthToken();
    }
}
