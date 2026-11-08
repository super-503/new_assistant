package com.zengame.ai.apiclient.bilibili;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 投稿列表接口要求的 dm_img 系列风控参数：模拟网页端的鼠标轨迹、WebGL 信息和页面元素位置，
 * 生成规则与 RSSHub bilibili 路由一致。
 */
public final class BilibiliDmParams {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private static final int TAG_DIV = 2;

    private BilibiliDmParams() {
    }

    /**
     * @return dm_img_list、dm_img_str、dm_cover_img_str、dm_img_inter 四个查询参数
     */
    public static Map<String, String> generate() {
        String noWebgl = truncatedBase64("no webgl");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("dm_img_list", imgList());
        params.put("dm_img_str", noWebgl);
        params.put("dm_cover_img_str", noWebgl);
        params.put("dm_img_inter", imgInter());
        return params;
    }

    private static String imgList() {
        long x = Math.max(gaussian(1245, 5), 0);
        long y = Math.max(gaussian(1285, 5), 0);
        ObjectNode point = JSON.objectNode()
                .put("x", 3 * x + 2 * y)
                .put("y", 4 * x - 5 * y)
                .put("z", 0)
                .put("timestamp", Math.max(gaussian(30, 5), 0))
                .put("type", 0);
        return JSON.arrayNode().add(point).toString();
    }

    private static String imgInter() {
        long[] p1 = interWh(274, 601);
        long[] s1 = interOf(134, 30);
        long[] p2 = interWh(332, 64);
        long[] s2 = interOf(1101, 338);
        ArrayNode ds = JSON.arrayNode()
                .add(interElement("clearfix g-search search-container", p1, s1))
                .add(interElement("wrapper", p2, s2));
        ObjectNode inter = JSON.objectNode();
        inter.set("ds", ds);
        inter.set("wh", longArray(interWh(1245, 1285)));
        inter.set("of", longArray(interOf(0, 0)));
        return inter.toString();
    }

    private static ObjectNode interElement(String className, long[] p, long[] s) {
        ObjectNode element = JSON.objectNode()
                .put("t", TAG_DIV)
                .put("c", truncatedBase64(className));
        element.set("p", longArray(new long[]{p[0], p[2], p[1]}));
        element.set("s", longArray(new long[]{s[2], s[0], s[1]}));
        return element;
    }

    private static long[] interOf(long top, long left) {
        long seed = ThreadLocalRandom.current().nextInt(514);
        return new long[]{3 * top + 2 * left + seed, 4 * top - 4 * left + 2 * seed, seed};
    }

    private static long[] interWh(long width, long height) {
        long seed = ThreadLocalRandom.current().nextInt(114);
        return new long[]{2 * width + 2 * height + 3 * seed, 4 * width - height + seed, seed};
    }

    private static ArrayNode longArray(long[] values) {
        ArrayNode array = JSON.arrayNode();
        for (long value : values) {
            array.add(value);
        }
        return array;
    }

    private static long gaussian(double mean, double std) {
        return Math.round(ThreadLocalRandom.current().nextGaussian() * std + mean);
    }

    /**
     * 网页端的写法：Base64 编码后去掉末尾两个字符。
     */
    private static String truncatedBase64(String text) {
        String encoded = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        return encoded.substring(0, encoded.length() - 2);
    }
}
