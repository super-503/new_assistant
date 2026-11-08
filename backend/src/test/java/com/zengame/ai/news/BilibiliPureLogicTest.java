package com.zengame.ai.news;

import com.zengame.ai.apiclient.bilibili.BilibiliCorrespondPath;
import com.zengame.ai.apiclient.bilibili.BilibiliWbiSign;
import com.zengame.ai.business.news.channel.bilibili.cover.BilibiliConvert;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B站插件的纯逻辑：WBI 签名、correspondPath 加密、时长解析、简介转 HTML。
 */
class BilibiliPureLogicTest {

    private static final Pattern HEX_1024_BIT = Pattern.compile("^[0-9a-f]{256}$");

    private final BilibiliConvert convert = BilibiliConvert.INSTANCE;

    @Test
    void mixinKeyMatchesKnownResult() {
        assertEquals("ea1db124af3c7062474693fa704f4ff8",
                BilibiliWbiSign.mixinKey("7cd084941338484aae1ad9425b84077c", "4932caff0ff746eab6f01bf08b70ac45"));
    }

    @Test
    void signQueryMatchesKnownResult() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("foo", "114");
        params.put("bar", "514");
        params.put("zab", "1919810");

        String query = BilibiliWbiSign.signQuery(params, "ea1db124af3c7062474693fa704f4ff8", 1702204169L);

        assertEquals("bar=514&foo=114&wts=1702204169&zab=1919810&w_rid=8f6f2b5b3d485fe1886cec6a0be8c5d4", query);
    }

    @Test
    void signQueryEncodesValuesAndDropsReservedChars() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("keyword", "影视 飓风!'()*");

        String query = BilibiliWbiSign.signQuery(params, "ea1db124af3c7062474693fa704f4ff8", 1702204169L);

        assertTrue(query.startsWith("keyword=%E5%BD%B1%E8%A7%86%20%E9%A3%93%E9%A3%8E&wts=1702204169&w_rid="), query);
    }

    @Test
    void correspondPathIsRsaOaepHex() {
        long timestamp = System.currentTimeMillis();
        String first = BilibiliCorrespondPath.generate(timestamp);
        String second = BilibiliCorrespondPath.generate(timestamp);

        assertTrue(HEX_1024_BIT.matcher(first).matches(), first);
        assertTrue(HEX_1024_BIT.matcher(second).matches(), second);
        assertNotEquals(first, second);
    }

    @Test
    void parsesDurationLength() {
        assertEquals(330, convert.parseDurationSeconds("05:30"));
        assertEquals(4530, convert.parseDurationSeconds("75:30"));
        assertEquals(3723, convert.parseDurationSeconds("1:02:03"));
        assertNull(convert.parseDurationSeconds(null));
        assertNull(convert.parseDurationSeconds(" "));
        assertThrows(NumberFormatException.class, () -> convert.parseDurationSeconds("ab:cd"));
    }

    @Test
    void descriptionBecomesEscapedParagraphs() {
        assertEquals("<p>新视频 &lt;上集&gt;</p><p>A &amp; B</p><p>&quot;完&quot;</p>",
                convert.descriptionToHtml("新视频 <上集>\r\n\nA & B\n\"完\""));
        assertNull(convert.descriptionToHtml(" \n "));
    }

    @Test
    void imageUrlsBecomeHttps() {
        assertEquals("https://i0.hdslb.com/a.jpg", convert.httpsUrl("http://i0.hdslb.com/a.jpg"));
        assertEquals("https://i0.hdslb.com/a.jpg", convert.httpsUrl("//i0.hdslb.com/a.jpg"));
        assertEquals("https://i0.hdslb.com/a.jpg", convert.httpsUrl("https://i0.hdslb.com/a.jpg"));
    }
}
