package com.zengame.ai.news;

import com.zengame.ai.business.news.channel.wechatmp.WechatMpArticleParser;
import com.zengame.ai.business.news.channel.wechatmp.WechatMpArticleUrl;
import com.zengame.ai.business.news.channel.wechatmp.bo.WechatMpArticlePage;
import com.zengame.ai.infrastructure.exception.BusinessException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公众号插件的纯逻辑：文章链接规范化，文章页正文修正和公众号信息解析。
 */
class WechatMpPureLogicTest {

    private static final String ARTICLE_URL = "https://mp.weixin.qq.com/s?__biz=MjM5NjYyMjM0MA==&mid=2650977243&idx=1&sn=7b9e86fe19423aa84d249fa6e951f6ea";

    @Test
    void canonicalizesLongLinkToEssentialParams() {
        String link = "http://mp.weixin.qq.com/s?__biz=MjM5NjYyMjM0MA==&amp;mid=2650977243&amp;idx=1"
                + "&amp;sn=7b9e86fe19423aa84d249fa6e951f6ea&amp;chksm=bd0a1b2b8a7d923d&amp;scene=21#wechat_redirect";

        assertEquals(ARTICLE_URL, WechatMpArticleUrl.canonicalize(link));
    }

    @Test
    void canonicalizationKeepsParamOrderRegardlessOfInput() {
        String link = "https://mp.weixin.qq.com/s?sn=7b9e86fe19423aa84d249fa6e951f6ea&idx=1&mid=2650977243"
                + "&__biz=MjM5NjYyMjM0MA==&chksm=bd0a1b2b8a7d923d#rd";

        assertEquals(ARTICLE_URL, WechatMpArticleUrl.canonicalize(link));
    }

    @Test
    void canonicalizesShortLinkToHttpsPath() {
        assertEquals("https://mp.weixin.qq.com/s/FnjcMXZ1xdS-d6n-pUUyyw",
                WechatMpArticleUrl.canonicalize("http://mp.weixin.qq.com/s/FnjcMXZ1xdS-d6n-pUUyyw?scene=1#rd"));
    }

    @Test
    void rejectsLinksThatAreNotArticles() {
        assertThrows(BusinessException.class, () -> WechatMpArticleUrl.canonicalize("https://example.com/s/abc"));
        assertThrows(BusinessException.class,
                () -> WechatMpArticleUrl.canonicalize("https://mp.weixin.qq.com/s?__biz=MjM5NjYyMjM0MA==&mid=1&idx=1"));
        assertThrows(BusinessException.class, () -> WechatMpArticleUrl.canonicalize("https://mp.weixin.qq.com/cgi-bin/home"));
    }

    @Test
    void requestUrlKeepsAllParamsAndDropsFragment() {
        String link = "http://mp.weixin.qq.com/s?__biz=MjM5NjYyMjM0MA==&amp;mid=2650977243&amp;idx=1"
                + "&amp;sn=7b9e86fe19423aa84d249fa6e951f6ea&amp;chksm=bd0a1b2b8a7d923d#rd";

        assertEquals(ARTICLE_URL + "&chksm=bd0a1b2b8a7d923d", WechatMpArticleUrl.toRequestUrl(link));
    }

    @Test
    void parsesAccountFromCgiData() throws IOException {
        WechatMpArticlePage page = WechatMpArticleParser.parse(loadSample(), ARTICLE_URL);

        assertEquals("测试公众号&实验室", page.getAccountName());
        assertEquals("http://mmbiz.qpic.cn/mmbiz_png/avatar/0?wx_fmt=png", page.getAccountAvatarUrl());
        assertNotNull(page.getBodyHtml());
    }

    @Test
    void fixesLazyImagesAndSections() throws IOException {
        Element body = fixedBody();

        Element img = body.selectFirst("img");
        assertNotNull(img);
        assertEquals("https://mmbiz.qpic.cn/mmbiz_jpg/lazy/640?wx_fmt=jpeg", img.attr("src"));
        assertFalse(img.hasAttr("data-src"));

        assertTrue(body.select("section").isEmpty());
        Element textOnly = body.selectFirst("p[style=text-align: center;]");
        assertNotNull(textOnly, body.html());
        assertEquals("纯文字段落", textOnly.text());
        Element outer = body.selectFirst("div.outer");
        assertNotNull(outer, body.html());
        assertEquals("p", outer.child(0).tagName(), "只包含图片的 section 转为 p");
    }

    @Test
    void convertsVoiceToAudio() throws IOException {
        Element body = fixedBody();

        assertTrue(body.select("mpvoice").isEmpty());
        Element audio = body.selectFirst("audio");
        assertNotNull(audio);
        assertEquals("https://res.wx.qq.com/voice/getvoice?mediaid=MjM5NjYyMjM0MF8yNjUwOTc3MjQy", audio.attr("src"));
        assertTrue(audio.hasAttr("controls"));
        assertEquals("一期 节目", audio.attr("title"));
    }

    @Test
    void fixesTencentVideoAndConvertsNativeVideos() throws IOException {
        Element body = fixedBody();

        Element tencent = body.selectFirst("iframe");
        assertNotNull(tencent);
        assertEquals("https://v.qq.com/txp/iframe/player.html?vid=q33310m040m&autoplay=false", tencent.attr("src"));
        assertFalse(tencent.hasAttr("data-src"));
        assertEquals("677", tencent.attr("width"));
        assertEquals("381", tencent.attr("height"));
        assertEquals(1, body.select("iframe").size(), "原生视频 iframe 已转为 video");

        Element video = body.selectFirst("video");
        assertNotNull(video);
        assertEquals("https://mpvideo.qpic.cn/nt.f10002.mp4?dis_k=7ec643&auth_key=7d488b", video.attr("src"));
        assertEquals("https://mmbiz.qpic.cn/mmbiz_jpg/cover/0?wx_fmt=jpeg", video.attr("poster"));
        assertTrue(video.hasAttr("controls"));
        assertEquals("https://mpvideo.qpic.cn/nt2.f10004.mp4?dis_k=small", body.select("video").get(1).attr("src"),
                "超过转存上限的清晰度取下一档");

        assertTrue(body.select("mpvideosnap, mp-common-videosnap").isEmpty());
        assertEquals(2, body.select("a[href=" + ARTICLE_URL + "]").size());
        body.select("a").forEach(link -> assertEquals("在原文中观看", link.text()));
    }

    @Test
    void nativeVideoWithoutPlayInfoFallsBackToOriginalLink() {
        Document document = Jsoup.parse("<div id=\"js_content\">"
                + "<iframe class=\"video_iframe\" data-mpvid=\"wxv_404\" "
                + "data-src=\"https://mp.weixin.qq.com/mp/readtemplate?t=pages/video_player_tmpl&amp;action=mpvideo\">"
                + "</iframe></div>");

        String fixed = WechatMpArticleParser.fixContent(document.getElementById("js_content"), ARTICLE_URL);

        Element link = Jsoup.parseBodyFragment(fixed).body().selectFirst("a");
        assertNotNull(link);
        assertEquals("在原文中观看", link.text());
        assertEquals(ARTICLE_URL, link.attr("href"));
    }

    @Test
    void fixesCodeBlocksAndRemovesScripts() throws IOException {
        Element body = fixedBody();

        assertTrue(body.select(".code-snippet__line-index").isEmpty());
        assertEquals(2, body.select("code + br").size());
        assertEquals("const a = 1;", body.select("code").get(0).text());
        assertTrue(body.select("script").isEmpty());
    }

    @Test
    void pageWithoutContentAreaHasNoBody() {
        Document document = Jsoup.parse("<html><head><script>window.cgiDataNew = { nick_name: '图片号', "
                + "round_head_img: 'http://mmbiz.qpic.cn/a/0' };</script></head><body><div class=\"share_media\"></div></body></html>");

        WechatMpArticlePage page = WechatMpArticleParser.parse(document, ARTICLE_URL);

        assertNull(page.getBodyHtml());
        assertEquals("图片号", page.getAccountName());
    }

    @Test
    void pageWithoutAccountInfoFails() {
        Document document = Jsoup.parse("<html><body><div id=\"js_content\"><p>正文</p></div></body></html>");

        assertThrows(BusinessException.class, () -> WechatMpArticleParser.parse(document, ARTICLE_URL));
    }

    private Element fixedBody() throws IOException {
        String fixed = WechatMpArticleParser.parse(loadSample(), ARTICLE_URL).getBodyHtml();
        return Jsoup.parseBodyFragment(fixed).body();
    }

    private Document loadSample() throws IOException {
        try (InputStream input = Objects.requireNonNull(
                getClass().getResourceAsStream("/news/wechatmp/js_content_sample.html"), "缺少测试样例")) {
            return Jsoup.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
