package com.zengame.ai.business.news.channel.x;

import com.zengame.ai.business.news.channel.x.bo.XMedia;
import com.zengame.ai.business.news.channel.x.bo.XPost;
import com.zengame.ai.business.news.channel.x.bo.XUrlEntity;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 把推文拼装成资讯正文 HTML 和纯文本。
 *
 * <p>正文规则参照 RSSHub 的推文拼装：
 * <ul>
 *     <li>文本：t.co 短链换成原始链接并加链接，@提及和话题加链接，删除指向媒体和指向推文自身的短链，换行转为 {@code <br>}；</li>
 *     <li>图片：{@code <img>} 原图；视频和 GIF：{@code <video>} 码率最高的 mp4 加封面，没有 mp4 时显示封面图；</li>
 *     <li>引用帖：正文后附 {@code <blockquote>}，含引用作者、文本、媒体和原帖链接。</li>
 * </ul>
 */
@Component
public class XPostRenderer {

    private static final String TCO_PREFIX = "https://t.co/";

    /**
     * 拼装推文正文 HTML。
     *
     * @param post 入库的推文（转推时为被转的原帖）
     * @return 正文 HTML
     */
    public String renderBody(XPost post) {
        Document document = Document.createShell("");
        document.outputSettings().prettyPrint(false);
        Element root = document.body();
        appendTextParagraph(root, post);
        appendMedia(root, post.getMedia());
        post.getQuoted().ifPresent(quoted -> appendQuote(root, quoted));
        return root.html();
    }

    /**
     * 推文的纯文本：短链换成原始链接，删除媒体短链，换行合并为空格。
     *
     * @param post 推文
     * @return 纯文本
     */
    public String renderPlainText(XPost post) {
        StringBuilder text = new StringBuilder();
        walkText(post, new TextSink() {
            @Override
            public void text(String value) {
                text.append(value);
            }

            @Override
            public void link(String href, String value) {
                text.append(value);
            }
        });
        return StringUtils.normalizeSpace(text.toString());
    }

    /**
     * 正文文本段落；文本为空（纯媒体推文）时不输出。
     */
    private void appendTextParagraph(Element parent, XPost post) {
        Element paragraph = new Element("p");
        walkText(post, new TextSink() {
            @Override
            public void text(String value) {
                String[] lines = value.split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    if (i > 0) {
                        paragraph.appendElement("br");
                    }
                    if (!lines[i].isEmpty()) {
                        paragraph.appendText(lines[i]);
                    }
                }
            }

            @Override
            public void link(String href, String value) {
                paragraph.appendElement("a").attr("href", href).text(value);
            }
        });
        if (paragraph.childNodeSize() > 0) {
            parent.appendChild(paragraph);
        }
    }

    /**
     * 每个媒体一个段落：图片为原图，视频和 GIF 为 mp4 加封面。
     */
    private void appendMedia(Element parent, List<XMedia> mediaList) {
        for (XMedia media : mediaList) {
            Element paragraph = parent.appendElement("p");
            Optional<String> videoUrl = media.getVideoUrl();
            if (videoUrl.isPresent()) {
                paragraph.appendElement("video")
                        .attr("src", videoUrl.get())
                        .attr("poster", media.getImageUrl())
                        .attr("controls", true);
            } else {
                paragraph.appendElement("img").attr("src", media.getImageUrl());
            }
        }
    }

    /**
     * 引用帖的引用块：作者、文本、媒体、原帖链接。
     */
    private void appendQuote(Element parent, XPost quoted) {
        Element blockquote = parent.appendElement("blockquote");
        Element author = blockquote.appendElement("p");
        author.appendElement("a").attr("href", "https://x.com/" + quoted.getAuthorScreenName()).text(quoted.getAuthorName());
        author.appendText(" @" + quoted.getAuthorScreenName());
        appendTextParagraph(blockquote, quoted);
        appendMedia(blockquote, quoted.getMedia());
        blockquote.appendElement("p").appendElement("a").attr("href", quoted.getPermalink()).text(quoted.getPermalink());
    }

    /**
     * 按实体切分正文，依次把普通文本和链接交给 sink。
     *
     * <p>先删除指向媒体和指向推文自身的 t.co 短链并去掉首尾空白，再匹配其余短链、@提及和话题；
     * 未出现在实体里的 @、# 文本按普通文本输出。
     */
    private void walkText(XPost post, TextSink sink) {
        String text = post.getText();
        for (XMedia media : post.getMedia()) {
            text = text.replace(media.getTcoUrl(), "");
        }
        String selfLinkSuffix = "/status/" + post.getRestId();
        for (XUrlEntity url : post.getUrls()) {
            if (url.getExpandedUrl().endsWith(selfLinkSuffix)) {
                text = text.replace(url.getUrl(), "");
            }
        }
        text = text.trim();

        Map<String, String> expandedUrlByTco = post.getUrls().stream()
                .collect(Collectors.toMap(XUrlEntity::getUrl, XUrlEntity::getExpandedUrl, (first, second) -> first));
        List<String> alternatives = new ArrayList<>();
        expandedUrlByTco.keySet().forEach(tco -> alternatives.add(Pattern.quote(tco)));
        post.getMentions().forEach(screenName -> alternatives.add("@(?i:" + Pattern.quote(screenName) + ")(?!\\w)"));
        post.getHashtags().forEach(tag -> alternatives.add("[#＃](?i:" + Pattern.quote(tag) + ")(?![\\p{L}\\p{N}_])"));
        if (alternatives.isEmpty()) {
            sink.text(text);
            return;
        }
        Matcher matcher = Pattern.compile(String.join("|", alternatives)).matcher(text);
        int last = 0;
        while (matcher.find()) {
            sink.text(text.substring(last, matcher.start()));
            String token = matcher.group();
            if (token.startsWith(TCO_PREFIX)) {
                String expandedUrl = expandedUrlByTco.get(token);
                sink.link(expandedUrl, expandedUrl);
            } else if (token.startsWith("@")) {
                sink.link("https://x.com/" + token.substring(1), token);
            } else {
                sink.link("https://x.com/hashtag/" + token.substring(1), token);
            }
            last = matcher.end();
        }
        sink.text(text.substring(last));
    }

    /**
     * 正文切分结果的接收方。
     */
    private interface TextSink {

        /**
         * @param value 普通文本，可能含换行
         */
        void text(String value);

        /**
         * @param href  链接地址
         * @param value 链接文本
         */
        void link(String href, String value);
    }
}
