package com.zengame.ai.news;

import com.zengame.ai.business.news.channel.ContentSanitizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 正文清洗：公众号风格的 HTML 经白名单过滤后，只留下允许的标签、属性和排版样式。
 */
class ContentSanitizerTest {

    private static final String WECHAT_STYLE_HTML = ""
            + "<section class=\"rich_media_content\" style=\"color: rgb(51, 51, 51); font-size: 15px; background-color: #fff;\">"
            + "<section style=\"text-align: center; margin: 0px 0px 16px; color: #ff0000;\">"
            + "<p class=\"js_darkmode\" style=\"text-align: center; font-family: PingFang SC; line-height: 1.75em; color: red;\">"
            + "<span style=\"font-size: 18px; color: #ff0000; font-weight: bold; background: yellow;\">居中标题</span>"
            + "</p>"
            + "</section>"
            + "<p style=\"color: #888888; font-size: 12px;\">只有颜色和字号的段落</p>"
            + "<p style=\"text-indent: 2em; letter-spacing: 1px; margin-top: 8px;\">首行缩进的段落</p>"
            + "<img class=\"rich_pages wxw-img\" data-src=\"https://mmbiz.qpic.cn/lazy.jpg\" src=\"https://mmbiz.qpic.cn/a.jpg\""
            + " style=\"width: 100%; max-width: 677px; border-radius: 4px; display: block; color: red;\" onerror=\"alert(1)\">"
            + "<script>alert('xss')</script>"
            + "<style>.rich_media_content { color: red; }</style>"
            + "<blockquote style=\"border: 1px solid #eee; padding: 8px; background-color: #f5f5f5;\">引用内容</blockquote>"
            + "<pre><code class=\"language-java\">System.out.println(1);</code></pre>"
            + "<iframe class=\"video_iframe\" src=\"https://v.qq.com/txp/iframe/player.html?vid=abc\" width=\"500\" height=\"300\""
            + " allowfullscreen=\"true\" frameborder=\"0\"></iframe>"
            + "<iframe src=\"https://evil.example.com/player\"></iframe>"
            + "<video src=\"https://cdn.example.com/v.mp4\" poster=\"https://cdn.example.com/p.jpg\" controls onclick=\"alert(1)\">"
            + "<source src=\"https://cdn.example.com/v.webm\" type=\"video/webm\"></video>"
            + "<a href=\"javascript:alert(1)\">坏链接</a><a href=\"https://mp.weixin.qq.com/s/abc\" class=\"link\">好链接</a>"
            + "</section>";

    private static final List<String> EMBED_DOMAINS = Collections.singletonList("v.qq.com");

    private final ContentSanitizer sanitizer = new ContentSanitizer();

    @Test
    void removesClassStyleTagScriptAndLayoutContainers() {
        String cleaned = sanitizer.sanitize(WECHAT_STYLE_HTML, EMBED_DOMAINS);
        Document doc = Jsoup.parseBodyFragment(cleaned);

        assertTrue(doc.select("[class]").isEmpty(), cleaned);
        assertTrue(doc.select("script, style, section").isEmpty(), cleaned);
        assertTrue(doc.select("[onerror], [onclick], [data-src]").isEmpty(), cleaned);
        assertFalse(cleaned.contains("alert"), cleaned);
        assertFalse(cleaned.contains("rich_media_content"), cleaned);
        assertEquals("居中标题", doc.selectFirst("p span").text());
        assertEquals("System.out.println(1);", doc.selectFirst("pre code").text());
    }

    @Test
    void keepsOnlyTypographyStyleProperties() {
        Document doc = Jsoup.parseBodyFragment(sanitizer.sanitize(WECHAT_STYLE_HTML, EMBED_DOMAINS));
        Elements paragraphs = doc.select("p");

        assertEquals("text-align:center;line-height:1.75em", paragraphs.get(0).attr("style"));
        assertEquals("font-weight:bold", paragraphs.get(0).selectFirst("span").attr("style"));
        assertFalse(paragraphs.get(1).hasAttr("style"), paragraphs.get(1).outerHtml());
        assertEquals("text-indent:2em;letter-spacing:1px;margin-top:8px", paragraphs.get(2).attr("style"));
        assertEquals("width:100%;max-width:677px;border-radius:4px;display:block", doc.selectFirst("img").attr("style"));
        assertEquals("border:1px solid #eee;padding:8px", doc.selectFirst("blockquote").attr("style"));
        assertEquals("https://mmbiz.qpic.cn/a.jpg", doc.selectFirst("img").attr("src"));
    }

    @Test
    void keepsLayoutLonghandsAndDropsBorderColorLonghands() {
        String html = "<blockquote style=\"margin-top: 8px; margin-right: 0; margin-bottom: 16px; margin-left: 0;"
                + " padding-top: 4px; padding-right: 8px; padding-bottom: 4px; padding-left: 12px;"
                + " border-left: 3px solid #07c160; border-top-width: 1px; border-top-style: dashed;"
                + " border-width: 2px; border-style: solid;"
                + " border-color: #ccc; border-left-color: #07c160; border-bottom-color: red;"
                + " border-top-left-radius: 4px; border-top-right-radius: 4px;"
                + " border-bottom-right-radius: 6px; border-bottom-left-radius: 6px;"
                + " color: #333; background-color: #f7f7f7;\">引用</blockquote>";

        Document doc = Jsoup.parseBodyFragment(sanitizer.sanitize(html, Collections.emptyList()));

        assertEquals("margin-top:8px;margin-right:0;margin-bottom:16px;margin-left:0;"
                        + "padding-top:4px;padding-right:8px;padding-bottom:4px;padding-left:12px;"
                        + "border-left:3px solid #07c160;border-top-width:1px;border-top-style:dashed;"
                        + "border-width:2px;border-style:solid;"
                        + "border-top-left-radius:4px;border-top-right-radius:4px;"
                        + "border-bottom-right-radius:6px;border-bottom-left-radius:6px",
                doc.selectFirst("blockquote").attr("style"));
    }

    @Test
    void removesStyleWhenOnlyBorderColorLonghandsRemain() {
        String html = "<p style=\"border-color: #ccc; border-top-color: red; border-right-color: blue;\">段落</p>";

        Document doc = Jsoup.parseBodyFragment(sanitizer.sanitize(html, Collections.emptyList()));

        assertFalse(doc.selectFirst("p").hasAttr("style"), doc.body().html());
    }

    @Test
    void keepsMediaAndOnlyDeclaredIframes() {
        Document doc = Jsoup.parseBodyFragment(sanitizer.sanitize(WECHAT_STYLE_HTML, EMBED_DOMAINS));

        Elements iframes = doc.select("iframe");
        assertEquals(1, iframes.size(), doc.body().html());
        Element iframe = iframes.first();
        assertEquals("https://v.qq.com/txp/iframe/player.html?vid=abc", iframe.attr("src"));
        assertEquals("500", iframe.attr("width"));
        assertEquals("300", iframe.attr("height"));
        assertTrue(iframe.hasAttr("allowfullscreen"));
        assertEquals("0", iframe.attr("frameborder"));

        Element video = doc.selectFirst("video");
        assertEquals("https://cdn.example.com/v.mp4", video.attr("src"));
        assertEquals("https://cdn.example.com/p.jpg", video.attr("poster"));
        assertTrue(video.hasAttr("controls"));
        assertEquals("https://cdn.example.com/v.webm", video.selectFirst("source").attr("src"));
        assertEquals("video/webm", video.selectFirst("source").attr("type"));

        Elements links = doc.select("a");
        assertFalse(links.get(0).hasAttr("href"), links.get(0).outerHtml());
        assertEquals("https://mp.weixin.qq.com/s/abc", links.get(1).attr("href"));
    }

    @Test
    void removesAllIframesWhenChannelDeclaresNoEmbedDomain() {
        Document doc = Jsoup.parseBodyFragment(sanitizer.sanitize(WECHAT_STYLE_HTML, Collections.emptyList()));

        assertTrue(doc.select("iframe").isEmpty(), doc.body().html());
    }
}
