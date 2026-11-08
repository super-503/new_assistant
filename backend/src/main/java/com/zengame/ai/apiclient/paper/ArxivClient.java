package com.zengame.ai.apiclient.paper;

import com.zengame.ai.business.paper.domain.enums.PaperSource;
import com.zengame.ai.business.paper.domain.vo.PaperHitVO;
import com.zengame.ai.business.paper.domain.vo.PaperSearchPageVO;
import com.zengame.ai.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** 通过 arXiv Atom API 检索预印本。 */
@Component
@RequiredArgsConstructor
public class ArxivClient {

    private static final HttpUrl SEARCH_URL = HttpUrl.get("https://export.arxiv.org/api/query");
    private static final long MIN_INTERVAL = TimeUnit.SECONDS.toNanos(3);

    private final PaperHttpClient httpClient;
    private long lastRequestNanos = System.nanoTime() - MIN_INTERVAL;
    private boolean requesting;

    public PaperSearchPageVO search(String keyword, String cursor) {
        int offset = PaperPageSupport.offsetOf(cursor);
        String phrase = StringUtils.normalizeSpace(keyword.replace('"', ' ').replace('\\', ' '));
        if (StringUtils.isBlank(phrase)) {
            throw new BusinessException("请输入 arXiv 论文关键词");
        }
        HttpUrl url = SEARCH_URL.newBuilder()
                .addQueryParameter("search_query", "all:\"" + phrase + "\"")
                .addQueryParameter("start", Integer.toString(offset))
                .addQueryParameter("max_results", Integer.toString(PaperPageSupport.PAGE_SIZE))
                .build();
        Request request = new Request.Builder().url(url).build();
        startRequest();
        try {
            return parse(httpClient.fetch(PaperSource.ARXIV, request), offset);
        } finally {
            finishRequest();
        }
    }

    PaperSearchPageVO parse(String xml, int offset) {
        Document document = Jsoup.parse(xml, "", Parser.xmlParser());
        long total = NumberUtils.toLong(document.getElementsByTag("opensearch:totalResults").text(), -1);
        if (total < 0) {
            throw PaperHttpClient.unavailable(PaperSource.ARXIV);
        }

        List<PaperHitVO> items = new ArrayList<>();
        for (Element entry : document.getElementsByTag("entry")) {
            HttpUrl url = HttpUrl.parse(entry.getElementsByTag("id").text());
            String title = entry.getElementsByTag("title").text();
            if (url == null || !url.host().equals("arxiv.org") || !url.encodedPath().startsWith("/abs/")
                    || StringUtils.isBlank(title)) {
                continue;
            }
            items.add(PaperHitVO.builder()
                    .title(title)
                    .authors(StringUtils.trimToNull(String.join(", ", entry.select("author > name").eachText())))
                    .publication(StringUtils.defaultIfBlank(entry.getElementsByTag("arxiv:journal_ref").text(), "arXiv"))
                    .publicationDate(StringUtils.substringBefore(entry.getElementsByTag("published").text(), "T"))
                    .summary(StringUtils.trimToNull(entry.getElementsByTag("summary").text()))
                    .doi(StringUtils.trimToNull(entry.getElementsByTag("arxiv:doi").text()))
                    .url(url.newBuilder().scheme("https").build().toString())
                    .build());
        }
        return new PaperSearchPageVO(items, PaperPageSupport.nextOffset(offset, total));
    }

    private synchronized void startRequest() {
        long now = System.nanoTime();
        if (requesting || now - lastRequestNanos < MIN_INTERVAL) {
            throw new BusinessException("arXiv 检索过于频繁，请稍后重试");
        }
        requesting = true;
        lastRequestNanos = now;
    }

    private synchronized void finishRequest() {
        requesting = false;
    }
}
