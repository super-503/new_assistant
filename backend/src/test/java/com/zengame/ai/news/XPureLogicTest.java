package com.zengame.ai.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zengame.ai.apiclient.x.XApiDocumentLoader;
import com.zengame.ai.apiclient.x.XGraphqlOperation;
import com.zengame.ai.apiclient.x.vo.XApiDocument;
import com.zengame.ai.apiclient.x.vo.XGraphqlEndpoint;
import com.zengame.ai.business.news.channel.bo.FetchedItem;
import com.zengame.ai.business.news.channel.bo.SourceCandidate;
import com.zengame.ai.business.news.channel.bo.StatItem;
import com.zengame.ai.business.news.channel.x.XPostRenderer;
import com.zengame.ai.business.news.channel.x.XTimelineParser;
import com.zengame.ai.business.news.channel.x.bo.XTimelineTweet;
import com.zengame.ai.business.news.channel.x.bo.XUserProfile;
import com.zengame.ai.business.news.channel.x.cover.XConvert;
import com.zengame.ai.business.news.domain.enums.NewsPlaybackKind;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * X 推文解析、正文拼装和接口文档解析：输入是经代理抓取的 X 网页端 GraphQL 真实响应和 TwitterInternalAPIDocument 的
 * API.json（test resources 下 news/x/），覆盖原创带图、转推、引用、视频、长推文、自己的回复串、实体展开，以及接口文档缺项时的失败。
 */
class XPureLogicTest {

    private static final String GITHUB_ID = "13334762";

    private static final String OPENAI_ID = "4398626122";

    private final XTimelineParser parser = new XTimelineParser();

    private final XPostRenderer renderer = new XPostRenderer();

    private final XApiDocumentLoader apiDocumentLoader = new XApiDocumentLoader();

    @Test
    void originalTweetWithPhotosExpandsEntities() {
        XTimelineTweet tweet = findTweet(githubTimeline(), "2115480207469560109");
        FetchedItem item = toItem(tweet);

        assertEquals("https://x.com/github/status/2115480207469560109", item.getCanonicalUrl());
        assertNull(item.getTitle());
        assertEquals("GitHub", item.getAuthor());
        assertEquals("https://pbs.twimg.com/profile_images/2051404708766507008/ATxxTJXO.jpg", item.getAuthorAvatarUrl());
        assertEquals(utc(2026, 10, 28, 16, 25, 59), item.getPublishTime());
        assertEquals(item.getPublishTime(), item.getFeedTime());
        assertEquals("https://pbs.twimg.com/media/HVuyCO2WIAATDlE?format=jpg&name=orig", item.getCoverUrl());
        assertNull(item.getPlayback());
        assertNull(item.getDurationSeconds());
        assertStats(item.getStats(), 623L, 111L);

        Document body = Jsoup.parseBodyFragment(item.getBodyHtml());
        assertFalse(item.getBodyHtml().contains("t.co/"), item.getBodyHtml());
        Element text = body.selectFirst("body > p");
        assertTrue(text.text().startsWith("Frontier performance at a fraction of the cost."), text.text());
        assertTrue(text.select("br").size() >= 2, item.getBodyHtml());
        Element mention = text.selectFirst("a[href=https://x.com/ArtificialAnlys]");
        assertEquals("@ArtificialAnlys", mention.text());
        String docsUrl = "https://docs.github.com/copilot/concepts/models/copilot-fusion"
                + "?utm_source=universe-2026-x&utm_medium=social&utm_campaign=universe-2026";
        assertEquals(docsUrl, text.selectFirst("a[href^=https://docs.github.com]").attr("href"));
        assertEquals(docsUrl, text.selectFirst("a[href^=https://docs.github.com]").text());
        assertEquals(Arrays.asList(
                        "https://pbs.twimg.com/media/HVuyCO2WIAATDlE?format=jpg&name=orig",
                        "https://pbs.twimg.com/media/HVuyCZMWMAAFNkI?format=jpg&name=orig"),
                body.select("img").eachAttr("src"));
        assertTrue(body.select("blockquote").isEmpty());

        String plainText = renderer.renderPlainText(tweet.getContent());
        assertTrue(plainText.startsWith("Frontier performance at a fraction of the cost. In the @ArtificialAnlys"), plainText);
        assertTrue(plainText.endsWith("Try it now. ⬇️ " + docsUrl), plainText);
    }

    @Test
    void retweetUsesOriginalPostButRetweetTime() {
        XTimelineTweet tweet = findTweet(githubTimeline(), "2117772073208779262");
        FetchedItem item = toItem(tweet);

        assertTrue(tweet.getRetweeted().isPresent());
        assertEquals("https://x.com/devdevcharlie/status/2117660975000322501", item.getCanonicalUrl());
        assertEquals("Charlie Gerard", item.getAuthor());
        assertEquals("https://pbs.twimg.com/profile_images/1687987966680154112/c99jXM9R.jpg", item.getAuthorAvatarUrl());
        assertEquals(utc(2026, 11, 3, 16, 51, 35), item.getPublishTime());
        assertEquals(utc(2026, 11, 4, 0, 13, 3), item.getFeedTime());
        assertStats(item.getStats(), 101L, 20L);
        assertEquals("<p>Built my first mechanical keyboard! Some mistakes were made but I got it all working eventually! 😂</p>"
                        + "<p><img src=\"https://pbs.twimg.com/media/HWNxboyaUAAKKyj?format=jpg&amp;name=orig\"></p>",
                item.getBodyHtml());

        Document body = Jsoup.parseBodyFragment(item.getBodyHtml());
        assertEquals("Built my first mechanical keyboard! Some mistakes were made but I got it all working eventually! 😂",
                body.selectFirst("body > p").text());
        assertFalse(item.getBodyHtml().contains("RT @"), item.getBodyHtml());
        assertEquals("https://pbs.twimg.com/media/HWNxboyaUAAKKyj?format=jpg&name=orig", body.selectFirst("img").attr("src"));
    }

    @Test
    void retweetOfQuoteWithVideoRendersVideoAndQuoteBlock() {
        FetchedItem item = toItem(findTweet(githubTimeline(), "2117715697119920304"));

        String mp4 = "https://video.twimg.com/amplify_video/2117324096866230273/vid/avc1/1842x1080/yHzea2lTtQBmXHgA.mp4?tag=29";
        String poster = "https://pbs.twimg.com/amplify_video_thumb/2117324096866230273/img/CBe7c0n5ddn1je01.jpg";
        assertEquals("https://x.com/moraes_c_/status/2117324202793488549", item.getCanonicalUrl());
        assertEquals("Camilla Moraes", item.getAuthor());
        assertEquals(NewsPlaybackKind.FILE, item.getPlayback().getKind());
        assertEquals(mp4, item.getPlayback().getUrl());
        assertEquals(poster, item.getPlayback().getCoverUrl());
        assertEquals(poster, item.getCoverUrl());
        assertEquals(12, item.getDurationSeconds());

        Document body = Jsoup.parseBodyFragment(item.getBodyHtml());
        Element video = body.selectFirst("body > p > video");
        assertEquals(mp4, video.attr("src"));
        assertEquals(poster, video.attr("poster"));
        assertTrue(video.hasAttr("controls"));

        Element quote = body.selectFirst("blockquote");
        Element quoteAuthor = quote.selectFirst("a[href=https://x.com/slicknet]");
        assertEquals("Nicholas C. Zakas", quoteAuthor.text());
        assertTrue(quote.text().contains("@slicknet"), quote.text());
        assertEquals("@moraes_c_", quote.selectFirst("a[href=https://x.com/moraes_c_]").text());
        assertEquals("@github", quote.selectFirst("a[href=https://x.com/github]").text());
        assertTrue(quote.text().contains("automatically allow PRs from people who have had at least X PRs merged already."),
                quote.text());
        assertEquals("https://x.com/slicknet/status/2058905457733841226",
                quote.select("a").last().attr("href"));
    }

    @Test
    void videoTweetLinksHashtagAndMention() {
        FetchedItem item = toItem(findTweet(githubTimeline(), "2117727321536729141"));

        String mp4 = "https://video.twimg.com/amplify_video/2117727283679043585/vid/avc1/1920x1080/xZyiNSfNif-nBfJT.mp4?tag=16";
        assertEquals(mp4, item.getPlayback().getUrl());
        assertEquals("https://pbs.twimg.com/media/HWOtxozX0AAd4mP?format=jpg&name=orig", item.getPlayback().getCoverUrl());
        assertEquals(158, item.getDurationSeconds());

        Document body = Jsoup.parseBodyFragment(item.getBodyHtml());
        assertFalse(item.getBodyHtml().contains("t.co/"), item.getBodyHtml());
        assertEquals("#GitHubUniverse", body.selectFirst("a[href=https://x.com/hashtag/GitHubUniverse]").text());
        assertEquals("@martinwoodward", body.selectFirst("a[href=https://x.com/martinwoodward]").text());
        assertEquals(mp4, body.selectFirst("video").attr("src"));
    }

    @Test
    void longformQuoteTweetUsesFullTextAndQuoteBlock() {
        FetchedItem item = toItem(findTweet(openaiTimeline(), "2113766354386084338"));

        assertEquals("https://x.com/OpenAI/status/2113766354386084338", item.getCanonicalUrl());
        Document body = Jsoup.parseBodyFragment(item.getBodyHtml());
        Element text = body.selectFirst("body > p");
        assertTrue(text.text().contains("Brotli cut new release versions and published a CVE."), text.text());
        assertTrue(text.text().contains("such as “curl --compressed”"), text.text());
        assertEquals("https://patchtheplanet.org/brotli", text.selectFirst("a").attr("href"));

        Element quote = body.selectFirst("blockquote");
        assertEquals("OpenAI", quote.selectFirst("a[href=https://x.com/OpenAI]").text());
        assertTrue(quote.text().contains("we have substantially hardened the security of our research training environment"),
                quote.text());
        assertEquals("https://openai.com/index/securing-our-frontier-model-training-environment/",
                quote.selectFirst("a[href^=https://openai.com]").attr("href"));
        assertEquals("https://x.com/OpenAI/status/2113718797505036665", quote.select("a").last().attr("href"));
    }

    @Test
    void timelineKeepsPinnedTweetAndOwnReplies() {
        List<XTimelineTweet> tweets = openaiTimeline();
        List<String> ids = tweets.stream().map(tweet -> tweet.getTweet().getRestId()).collect(Collectors.toList());

        assertEquals(ids.size(), ids.stream().distinct().count());
        assertTrue(ids.contains("2114780711664398349"), "缺少置顶推文");
        assertTrue(ids.contains("2117692480590373245"), "缺少回复串里的回复");
        assertTrue(tweets.stream().allMatch(tweet -> "OpenAI".equals(tweet.getTweet().getAuthorScreenName())));

        FetchedItem reply = toItem(findTweet(tweets, "2117692483685757188"));
        assertEquals("https://x.com/OpenAI/status/2117692483685757188", reply.getCanonicalUrl());
        assertEquals(reply.getPublishTime(), reply.getFeedTime());

        List<XTimelineTweet> githubTweets = githubTimeline();
        assertEquals(20, githubTweets.size());
        assertEquals(9, githubTweets.stream().filter(tweet -> tweet.getRetweeted().isPresent()).count());
    }

    @Test
    void parsesUserProfile() {
        XUserProfile profile = parser.parseUser(readFixture("user-by-screen-name-openai.json"))
                .orElseThrow(() -> new AssertionError("未解析出用户"));
        SourceCandidate candidate = XConvert.INSTANCE.toSourceCandidate(profile);

        assertEquals(OPENAI_ID, candidate.getSourceKey());
        assertEquals("OpenAI", candidate.getName());
        assertEquals("@OpenAI", candidate.getHandle());
        assertEquals("https://pbs.twimg.com/profile_images/1885410181409820672/ztsaR0JW.jpg", candidate.getAvatarUrl());
        assertEquals("OpenAI’s mission is to ensure that artificial general intelligence benefits all of humanity. "
                + "We’re hiring: http://openai.com/jobs", candidate.getDescription());
        assertEquals(1, candidate.getStats().size());
        assertEquals("关注者", candidate.getStats().get(0).getName());
        assertEquals(5502312L, candidate.getStats().get(0).getValue());
    }

    /**
     * 搜索「tibo」：搜索框用户联想按顺序给出账号 id，再按 rest_id 查到 thsottiaux 的完整资料。
     */
    @Test
    void parsesTypeaheadAndSearchedUser() {
        List<String> ids = parser.parseTypeaheadUserIds(readFixture("typeahead-users-tibo.json"));

        assertEquals(9, ids.size());
        assertEquals("1953337039510003712", ids.get(0));

        SourceCandidate tibo = XConvert.INSTANCE.toSourceCandidate(parser.parseUser(readFixture("user-by-rest-id-thsottiaux.json"))
                .orElseThrow(() -> new AssertionError("未解析出用户")));
        assertEquals("1953337039510003712", tibo.getSourceKey());
        assertEquals("Tibo", tibo.getName());
        assertEquals("@thsottiaux", tibo.getHandle());
        assertEquals("https://pbs.twimg.com/profile_images/2093807917833281537/2yBgpwVV.jpg", tibo.getAvatarUrl());
        assertEquals("Codex & ChatGPT @OpenAI", tibo.getDescription());
        assertEquals(985100L, tibo.getStats().get(0).getValue());
    }

    @Test
    void missingUserParsesToEmpty() {
        Optional<XUserProfile> profile = parser.parseUser(JacksonUtil.readTree("{\"data\":{}}"));

        assertFalse(profile.isPresent());
    }

    @Test
    void parsesApiDocument() {
        XApiDocument document = apiDocumentLoader.parse(readFixtureText("twitter-internal-api-document.json"));

        assertEquals("Bearer AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA",
                document.getBearerToken());
        assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/157.0.0.0 Safari/537.36",
                document.getUserAgent());
        assertEndpoint(document.endpointOf(XGraphqlOperation.VIEWER), "TpQxQtWMTMnIAMPlMGMqNw", "Viewer", 7);
        assertEndpoint(document.endpointOf(XGraphqlOperation.USER_BY_SCREEN_NAME), "AMIBMjtxEEATh4z8V9GtRg", "UserByScreenName", 12);
        assertEndpoint(document.endpointOf(XGraphqlOperation.USER_BY_REST_ID), "mxaYWwt52eAb6OAzYHMVzg", "UserByRestId", 10);
        assertEndpoint(document.endpointOf(XGraphqlOperation.USER_TWEETS), "Rf0Oc_XSVYjASOj-Jbz3Xg", "UserTweets", 40);
        assertTrue(document.endpointOf(XGraphqlOperation.USER_TWEETS).getFeatures()
                .path("longform_notetweets_consumption_enabled").asBoolean());
    }

    @Test
    void apiDocumentMissingOperationFails() {
        ObjectNode document = (ObjectNode) JacksonUtil.readTree(readFixtureText("twitter-internal-api-document.json"));
        ((ObjectNode) document.get("graphql")).remove("UserTweets");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> apiDocumentLoader.parse(document.toString()));

        assertEquals("X 接口文档缺少 UserTweets", exception.getMessage());
    }

    @Test
    void apiDocumentMissingViewerFails() {
        ObjectNode document = (ObjectNode) JacksonUtil.readTree(readFixtureText("twitter-internal-api-document.json"));
        ((ObjectNode) document.get("graphql")).remove("Viewer");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> apiDocumentLoader.parse(document.toString()));

        assertEquals("X 接口文档缺少 Viewer", exception.getMessage());
    }

    @Test
    void apiDocumentMissingBearerFails() {
        ObjectNode document = (ObjectNode) JacksonUtil.readTree(readFixtureText("twitter-internal-api-document.json"));
        ((ObjectNode) document.get("header")).remove("authorization");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> apiDocumentLoader.parse(document.toString()));

        assertEquals("X 接口文档缺少 header.authorization", exception.getMessage());
    }

    private static void assertEndpoint(XGraphqlEndpoint endpoint, String queryId, String operationName, int featureCount) {
        assertEquals(queryId, endpoint.getQueryId());
        assertEquals("https://x.com/i/api/graphql/" + queryId + "/" + operationName, endpoint.getUrl());
        assertEquals(featureCount, endpoint.getFeatures().size());
    }

    private FetchedItem toItem(XTimelineTweet tweet) {
        return XConvert.INSTANCE.toFetchedItem(tweet, renderer.renderBody(tweet.getContent()));
    }

    private List<XTimelineTweet> githubTimeline() {
        return parser.parseUserTweets(readFixture("user-tweets-github.json"), GITHUB_ID);
    }

    private List<XTimelineTweet> openaiTimeline() {
        return parser.parseUserTweets(readFixture("user-tweets-openai.json"), OPENAI_ID);
    }

    private static XTimelineTweet findTweet(List<XTimelineTweet> tweets, String restId) {
        return tweets.stream()
                .filter(tweet -> restId.equals(tweet.getTweet().getRestId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("时间线里没有推文 " + restId));
    }

    private static void assertStats(List<StatItem> stats, Long likes, Long replies) {
        assertEquals(2, stats.size());
        assertEquals("点赞", stats.get(0).getName());
        assertEquals(likes, stats.get(0).getValue());
        assertEquals("回复", stats.get(1).getName());
        assertEquals(replies, stats.get(1).getValue());
    }

    private static LocalDateTime utc(int year, int month, int day, int hour, int minute, int second) {
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private static JsonNode readFixture(String name) {
        return JacksonUtil.readTree(readFixtureText(name));
    }

    private static String readFixtureText(String name) {
        try (InputStream input = Objects.requireNonNull(
                XPureLogicTest.class.getResourceAsStream("/news/x/" + name), "缺少测试数据 " + name)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
