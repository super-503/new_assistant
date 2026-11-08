package com.zengame.ai.apiclient.bilibili;

import org.springframework.util.DigestUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * B站网页端接口的 WBI 签名：由 nav 接口下发的 img_key、sub_key 打乱出 mixin key，
 * 再对按参数名排序的查询串加上 mixin key 取 MD5，得到 w_rid。
 */
public final class BilibiliWbiSign {

    private static final int[] MIXIN_KEY_ENC_TAB = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
            33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
            61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
            36, 20, 34, 44, 52
    };

    private static final int MIXIN_KEY_LENGTH = 32;

    private static final String VALUE_FILTERED_CHARS = "[!'()*]";

    private BilibiliWbiSign() {
    }

    /**
     * 由 img_key、sub_key 计算 mixin key。
     *
     * @param imgKey nav 接口 wbi_img.img_url 的文件名
     * @param subKey nav 接口 wbi_img.sub_url 的文件名
     * @return 32 位 mixin key
     */
    public static String mixinKey(String imgKey, String subKey) {
        String raw = imgKey + subKey;
        StringBuilder mixed = new StringBuilder();
        for (int index : MIXIN_KEY_ENC_TAB) {
            mixed.append(raw.charAt(index));
        }
        return mixed.substring(0, MIXIN_KEY_LENGTH);
    }

    /**
     * 给查询参数签名。
     *
     * @param params   原始查询参数，不含 wts、w_rid
     * @param mixinKey {@link #mixinKey(String, String)} 的结果
     * @param wts      当前秒级时间戳
     * @return 签名后的查询串，含 wts 和 w_rid，可直接拼在接口地址的 ? 之后
     */
    public static String signQuery(Map<String, String> params, String mixinKey, long wts) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        sorted.put("wts", String.valueOf(wts));
        String query = sorted.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue().replaceAll(VALUE_FILTERED_CHARS, "")))
                .collect(Collectors.joining("&"));
        String wRid = DigestUtils.md5DigestAsHex((query + mixinKey).getBytes(StandardCharsets.UTF_8));
        return query + "&w_rid=" + wRid;
    }

    private static String encode(String value) {
        return UriUtils.encode(value, StandardCharsets.UTF_8);
    }
}
