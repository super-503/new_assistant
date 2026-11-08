package com.zengame.ai.apiclient.paper;

import com.fasterxml.jackson.databind.JsonNode;
import com.zengame.ai.business.paper.domain.enums.PaperSource;
import com.zengame.ai.business.paper.domain.vo.PaperHitVO;
import com.zengame.ai.business.paper.domain.vo.PaperSearchPageVO;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 检索 Europe PMC 的医学与生命科学题录。 */
@Component
@RequiredArgsConstructor
public class EuropePmcClient {

    private static final HttpUrl SEARCH_URL =
            HttpUrl.get("https://www.ebi.ac.uk/europepmc/webservices/rest/search");
    private static final HttpUrl ARTICLE_URL = HttpUrl.get("https://europepmc.org/article");

    private final PaperHttpClient httpClient;

    public PaperSearchPageVO search(String keyword, String cursor) {
        String requestedCursor = StringUtils.defaultIfBlank(cursor, "*");
        HttpUrl url = SEARCH_URL.newBuilder()
                .addQueryParameter("query", keyword)
                .addQueryParameter("format", "json")
                .addQueryParameter("resultType", "core")
                .addQueryParameter("pageSize", Integer.toString(PaperPageSupport.PAGE_SIZE))
                .addQueryParameter("cursorMark", requestedCursor)
                .build();
        Request request = new Request.Builder().url(url).build();
        return parse(httpClient.fetch(PaperSource.EUROPE_PMC, request), requestedCursor);
    }

    PaperSearchPageVO parse(String json, String requestedCursor) {
        JsonNode payload = JacksonUtil.readTree(json);
        JsonNode records = payload.path("resultList").path("result");
        if (!records.isArray()) {
            throw PaperHttpClient.unavailable(PaperSource.EUROPE_PMC);
        }

        List<PaperHitVO> items = new ArrayList<>();
        for (JsonNode record : records) {
            String title = PaperPageSupport.plainText(record.path("title").asText(null));
            String id = record.path("id").asText(null);
            String source = record.path("source").asText(null);
            if (StringUtils.isAnyBlank(title, id, source)) {
                continue;
            }
            items.add(PaperHitVO.builder()
                    .title(title)
                    .authors(record.path("authorString").asText(null))
                    .publication(record.path("journalInfo").path("journal").path("title").asText(null))
                    .publicationDate(StringUtils.firstNonBlank(record.path("firstPublicationDate").asText(null),
                            record.path("pubYear").asText(null)))
                    .summary(PaperPageSupport.plainText(record.path("abstractText").asText(null)))
                    .doi(record.path("doi").asText(null))
                    .url(ARTICLE_URL.newBuilder().addPathSegment(source).addPathSegment(id).build().toString())
                    .build());
        }
        String nextCursor = payload.path("nextCursorMark").asText(null);
        if (records.size() < PaperPageSupport.PAGE_SIZE || StringUtils.equals(nextCursor, requestedCursor)) {
            nextCursor = null;
        }
        return new PaperSearchPageVO(items, StringUtils.trimToNull(nextCursor));
    }
}
