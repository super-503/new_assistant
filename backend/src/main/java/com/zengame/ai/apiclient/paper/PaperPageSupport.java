package com.zengame.ai.apiclient.paper;

import com.zengame.ai.infrastructure.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;

final class PaperPageSupport {

    static final int PAGE_SIZE = 20;
    private static final int MAX_OFFSET = 980;

    private PaperPageSupport() {
    }

    static int offsetOf(String cursor) {
        if (StringUtils.isBlank(cursor)) {
            return 0;
        }
        int offset = NumberUtils.toInt(cursor, -1);
        if (offset < 0 || offset > MAX_OFFSET || offset % PAGE_SIZE != 0) {
            throw new BusinessException("论文分页游标无效");
        }
        return offset;
    }

    static String nextOffset(int offset, long total) {
        int next = offset + PAGE_SIZE;
        return next <= MAX_OFFSET && next < total ? Integer.toString(next) : null;
    }

    static String plainText(String markup) {
        if (StringUtils.isBlank(markup)) {
            return null;
        }
        return StringUtils.trimToNull(Jsoup.parseBodyFragment(markup).text());
    }
}
