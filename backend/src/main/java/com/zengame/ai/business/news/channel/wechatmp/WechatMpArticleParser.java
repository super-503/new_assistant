package com.zengame.ai.business.news.channel.wechatmp;

import com.zengame.ai.business.news.channel.wechatmp.bo.WechatMpArticlePage;
import com.zengame.ai.infrastructure.exception.BusinessException;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公众号文章页解析：取所属公众号，取 {@code #js_content} 正文并修正为通用 HTML。
 *
 * <p>正文修正参照 RSSHub 的 {@code fixArticleContent}：懒加载图片、音频、腾讯视频、公众号原生视频、
 * {@code section}、代码块、脚本。原生视频的播放地址取自页面里的 {@code window.__videoPageInfos}，
 * 修正成使用渠道地址的 {@code video} 标签；取不到播放信息或视频号视频时替换为指向原文的
 * 「在原文中观看」链接。
 */
public final class WechatMpArticleParser {

    private static final String AUDIO_URL = "https://res.wx.qq.com/voice/getvoice?mediaid=";

    private static final String TENCENT_VIDEO_HOST = "v.qq.com";

    private static final String TENCENT_PLAYER_URL = "https://v.qq.com/txp/iframe/player.html";

    private static final int MAX_VIDEO_WIDTH = 677;

    /**
     * 公众号原生视频转存时的单文件上限，超过后改取更小的清晰度
     */
    private static final long MAX_NATIVE_VIDEO_BYTES = 50L * 1024 * 1024;

    private static final String VIDEO_INFOS_START = "var videoPageInfos = [";

    private static final String VIDEO_INFOS_END = "window.__videoPageInfos";

    private static final Pattern VIDEO_ID = Pattern.compile("video_id:\\s*'([^']*)'");

    private static final Pattern VIDEO_COVER = Pattern.compile("cover_url:\\s*'([^']*)'");

    /**
     * 一个转码版本的文件大小和播放地址；同一视频有多个清晰度，每个版本是一个对象
     */
    private static final Pattern VIDEO_FILE = Pattern.compile(
            "filesize:\\s*'?(\\d+)'?[^{}]*?url:\\s*\\(?\\s*'([^']+)'");

    private static final Pattern NICK_NAME = Pattern.compile("\\bnick_name:\\s*'([^']*)'");

    private static final Pattern ROUND_HEAD_IMG = Pattern.compile("\\bround_head_img:\\s*'([^']*)'");

    private WechatMpArticleParser() {
    }

    /**
     * 解析文章页。
     *
     * @param document   文章页文档
     * @param articleUrl 原文链接，原生视频替换成的链接指向它
     * @return 修正后的正文和所属公众号；页面没有正文区时正文为空
     * @throws BusinessException 页面里没有公众号信息
     */
    public static WechatMpArticlePage parse(Document document, String articleUrl) {
        document.outputSettings().prettyPrint(false);
        String cgiData = Optional.ofNullable(document.selectFirst("script:containsData(window.cgiDataNew =)"))
                .map(Element::data)
                .orElseThrow(() -> new BusinessException("公众号文章页缺少公众号信息：" + articleUrl));
        String bodyHtml = Optional.ofNullable(document.selectFirst("#js_content"))
                .map(content -> fixContent(content, articleUrl))
                .filter(StringUtils::isNotBlank)
                .orElse(null);
        return WechatMpArticlePage.builder()
                .bodyHtml(bodyHtml)
                .accountName(extractCgiField(cgiData, NICK_NAME, "nick_name", articleUrl))
                .accountAvatarUrl(extractCgiField(cgiData, ROUND_HEAD_IMG, "round_head_img", articleUrl))
                .build();
    }

    /**
     * 修正 {@code #js_content} 正文。
     *
     * @param content    正文区元素，修正直接作用在它上面
     * @param articleUrl 原文链接，原生视频替换成的链接指向它
     * @return 修正后的正文 HTML（正文区的内部 HTML）
     */
    public static String fixContent(Element content, String articleUrl) {
        Document document = Objects.requireNonNull(content.ownerDocument(), "正文区必须属于一个文档");
        document.outputSettings().prettyPrint(false);

        content.select("img[data-src]").forEach(img -> img.attr("src", img.attr("data-src")).removeAttr("data-src"));

        content.select("mpvoice[voice_encode_fileid]").forEach(voice -> voice.replaceWith(new Element("audio")
                .attr("controls", true)
                .attr("src", AUDIO_URL + voice.attr("voice_encode_fileid"))
                .attr("title", StringUtils.defaultIfBlank(voice.attr("name"), "音频"))
                .attr("style", "width:100%")));

        List<Element> videoIframes = content.select("iframe.video_iframe");
        Map<String, NativeVideo> nativeVideos = videoIframes.isEmpty() ? Map.of() : nativeVideos(document);
        for (Element iframe : videoIframes) {
            Optional<String> tencentVideoId = tencentVideoId(iframe.attr("data-src"));
            if (tencentVideoId.isPresent()) {
                fixTencentVideo(iframe, tencentVideoId.get());
                continue;
            }
            Optional<NativeVideo> nativeVideo = Optional.ofNullable(nativeVideos.get(iframe.attr("data-mpvid")));
            if (nativeVideo.isPresent()) {
                fixNativeVideo(iframe, nativeVideo.get());
            } else {
                iframe.replaceWith(watchInOriginalLink(articleUrl));
            }
        }
        content.select("mpvideo, mpvideosnap, mp-common-videosnap")
                .forEach(video -> video.replaceWith(watchInOriginalLink(articleUrl)));

        for (Element section : content.select("section")) {
            boolean containsBlock = section.select("p, div, section").stream().anyMatch(child -> child != section);
            section.tagName(containsBlock ? "div" : "p");
        }

        content.select("code").forEach(code -> code.after(new Element("br")));
        content.select(".code-snippet__line-index").remove();
        content.select("script").remove();
        return content.html();
    }

    /**
     * 腾讯视频 iframe 的 data-src 形如 {@code https://v.qq.com/iframe/preview.html?vid=xxx}，取其中的 vid。
     */
    private static Optional<String> tencentVideoId(String dataSrc) {
        return Optional.of(dataSrc)
                .filter(StringUtils::isNotBlank)
                .map(src -> UriComponentsBuilder.fromUriString(src).build())
                .filter(uri -> TENCENT_VIDEO_HOST.equals(uri.getHost()))
                .map(UriComponents::getQueryParams)
                .map(params -> params.getFirst("vid"))
                .filter(StringUtils::isNotBlank);
    }

    /**
     * 腾讯视频 iframe 改为可直接播放的播放器地址，并按 data-w、data-ratio 设置宽高。
     */
    private static void fixTencentVideo(Element iframe, String videoId) {
        iframe.attr("src", UriComponentsBuilder.fromHttpUrl(TENCENT_PLAYER_URL)
                .queryParam("vid", videoId)
                .queryParam("autoplay", "false")
                .build()
                .toUriString());
        iframe.removeAttr("data-src");
        String width = iframe.attr("data-w");
        String ratio = iframe.attr("data-ratio");
        if (NumberUtils.isCreatable(width) && NumberUtils.isCreatable(ratio)) {
            int fixedWidth = Math.min(NumberUtils.createNumber(width).intValue(), MAX_VIDEO_WIDTH);
            iframe.attr("width", String.valueOf(fixedWidth));
            iframe.attr("height", String.valueOf(Math.round(fixedWidth / NumberUtils.createDouble(ratio))));
        }
    }

    /**
     * 从文章页脚本里的 {@code window.__videoPageInfos} 取每个原生视频的播放地址和封面。
     *
     * @param document 文章页文档
     * @return 微信读书视频编号到播放信息的映射；页面没有原生视频或取不到播放信息时为空
     */
    private static Map<String, NativeVideo> nativeVideos(Document document) {
        return document.select("script").stream()
                .map(Element::data)
                .filter(script -> script.contains(VIDEO_INFOS_START))
                .findFirst()
                .map(WechatMpArticleParser::parseNativeVideos)
                .orElseGet(Map::of);
    }

    private static Map<String, NativeVideo> parseNativeVideos(String script) {
        String infos = StringUtils.substringBetween(script, VIDEO_INFOS_START, VIDEO_INFOS_END);
        Map<String, NativeVideo> videos = new HashMap<>();
        if (StringUtils.isBlank(infos)) {
            return videos;
        }
        String[] entries = infos.split("video_id:\\s*'");
        for (int i = 1; i < entries.length; i++) {
            String entry = entries[i];
            String videoId = StringUtils.substringBefore(entry, "'");
            nativeVideoUrl(entry).ifPresent(url -> videos.put(videoId, new NativeVideo(url, nativeVideoCover(entry))));
        }
        return videos;
    }

    /**
     * 同一视频有多个清晰度，按文件大小挑一个：优先取不超过上限的最大文件，全部超过上限时取最小的。
     */
    private static Optional<String> nativeVideoUrl(String entry) {
        Matcher matcher = VIDEO_FILE.matcher(entry);
        String largestWithinLimit = null;
        long largestWithinLimitSize = -1;
        String smallest = null;
        long smallestSize = Long.MAX_VALUE;
        while (matcher.find()) {
            long size = Long.parseLong(matcher.group(1));
            String url = decodeUrl(matcher.group(2));
            if (size <= MAX_NATIVE_VIDEO_BYTES && size > largestWithinLimitSize) {
                largestWithinLimitSize = size;
                largestWithinLimit = url;
            }
            if (size < smallestSize) {
                smallestSize = size;
                smallest = url;
            }
        }
        return Optional.ofNullable(largestWithinLimit != null ? largestWithinLimit : smallest);
    }

    private static String nativeVideoCover(String entry) {
        Matcher matcher = VIDEO_COVER.matcher(entry);
        return matcher.find() ? decodeUrl(matcher.group(1)) : null;
    }

    /**
     * 播放地址在页面脚本里用 {@code \x26} 转义并保留 HTML 实体，页面统一按 https 播放。
     */
    private static String decodeUrl(String url) {
        return url.replace("\\x26", "&").replace("&amp;", "&").replaceFirst("^http://", "https://");
    }

    /**
     * 原生视频用渠道播放地址和封面生成 {@code video} 标签。
     */
    private static void fixNativeVideo(Element iframe, NativeVideo video) {
        iframe.tagName("video");
        iframe.attr("src", video.getUrl());
        iframe.attr("controls", true);
        Optional.ofNullable(video.getCoverUrl())
                .filter(StringUtils::isNotBlank)
                .ifPresent(cover -> iframe.attr("poster", cover));
        iframe.removeAttr("data-src");
        iframe.removeAttr("data-mpvid");
    }

    private static Element watchInOriginalLink(String articleUrl) {
        return new Element("a").attr("href", articleUrl).text("在原文中观看");
    }

    /**
     * 从文章页 {@code window.cgiDataNew} 脚本中取一个字符串字段，第一个出现的即为文章所属公众号的值。
     */
    private static String extractCgiField(String cgiData, Pattern pattern, String field, String articleUrl) {
        Matcher matcher = pattern.matcher(cgiData);
        if (!matcher.find() || StringUtils.isBlank(matcher.group(1))) {
            throw new BusinessException(String.format("公众号文章页缺少 %s：%s", field, articleUrl));
        }
        return Parser.unescapeEntities(matcher.group(1), false);
    }

    /**
     * 公众号原生视频的播放地址和封面。
     */
    @Value
    private static class NativeVideo {
        String url;
        String coverUrl;
    }
}
