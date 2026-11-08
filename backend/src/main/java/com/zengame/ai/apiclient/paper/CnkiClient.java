package com.zengame.ai.apiclient.paper;

import com.zengame.ai.business.paper.domain.enums.PaperSource;
import com.zengame.ai.business.paper.domain.vo.PaperHitVO;
import com.zengame.ai.business.paper.domain.vo.PaperSearchPageVO;
import lombok.RequiredArgsConstructor;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** 检索知网空间公开的文献列表。 */
@Component
@RequiredArgsConstructor
public class CnkiClient {

    private static final String SEARCH_URL = "https://search.cnki.com.cn/search/listresult";

    private final PaperHttpClient httpClient;

    public PaperSearchPageVO search(String keyword, String cursor) {
        int offset = PaperPageSupport.offsetOf(cursor);
        FormBody form = new FormBody.Builder()
                .add("Content", keyword)
                .add("SearchType", "MulityTermsSearch")
                .add("ArticleType", "0")
                .add("Order", "1")
                .add("Page", Integer.toString(offset / PaperPageSupport.PAGE_SIZE + 1))
                .build();
        Request request = new Request.Builder().url(SEARCH_URL).post(form).build();
        return parse(httpClient.fetch(PaperSource.CNKI, request), offset);
    }

    PaperSearchPageVO parse(String html, int offset) {
        Document document = Jsoup.parse(html, SEARCH_URL);
        if (document.selectFirst(".no-result") != null) {
            return new PaperSearchPageVO(List.of(), null);
        }
        Element count = document.selectFirst("#hidTotalCount");
        long total = count == null ? -1 : NumberUtils.toLong(count.val(), -1);
        if (total < 0) {
            throw PaperHttpClient.unavailable(PaperSource.CNKI);
        }
        List<PaperHitVO> items = document.select(".lplist .list-item").stream()
                .map(this::toHit)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (items.isEmpty() && total > offset) {
            throw PaperHttpClient.unavailable(PaperSource.CNKI);
        }
        return new PaperSearchPageVO(items, PaperPageSupport.nextOffset(offset, total));
    }

    private PaperHitVO toHit(Element item) {
        Element anchor = item.selectFirst(".tit a[title][href]:not(.download)");
        if (anchor == null || StringUtils.isBlank(anchor.attr("title"))) {
            return null;
        }
        HttpUrl url = HttpUrl.parse(anchor.absUrl("href"));
        if (url == null || !isCnkiHost(url.host())) {
            return null;
        }
        String authors = item.select(".source a[data-key]").stream()
                .map(Element::text)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("、"));
        Element source = item.selectFirst(".source");
        Elements metadata = source == null ? new Elements() : source.children();
        return PaperHitVO.builder()
                .title(anchor.attr("title"))
                .authors(StringUtils.trimToNull(authors))
                .publication(metadataText(metadata, 3))
                .publicationDate(metadataText(metadata, 2))
                .summary(StringUtils.trimToNull(item.select(".nr").text()))
                .url(url.newBuilder().scheme("https").build().toString())
                .build();
    }

    private static boolean isCnkiHost(String host) {
        return host.equals("cnki.net") || host.endsWith(".cnki.net")
                || host.equals("cnki.com.cn") || host.endsWith(".cnki.com.cn");
    }

    private static String metadataText(Elements metadata, int positionFromEnd) {
        int index = metadata.size() - positionFromEnd;
        return index > 0 ? StringUtils.trimToNull(metadata.get(index).text()) : null;
    }
}
