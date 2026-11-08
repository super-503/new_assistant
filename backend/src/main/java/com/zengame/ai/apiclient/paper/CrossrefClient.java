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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** 从 Crossref 检索期刊、会议、学位论文和预印本的 DOI 题录。 */
@Component
@RequiredArgsConstructor
public class CrossrefClient {

    private static final HttpUrl WORKS_URL = HttpUrl.get("https://api.crossref.org/works");
    private static final String PAPER_TYPES =
            "type:journal-article,type:proceedings-article,type:dissertation,type:posted-content";

    private final PaperHttpClient httpClient;

    public PaperSearchPageVO search(String keyword, String cursor) {
        int offset = PaperPageSupport.offsetOf(cursor);
        HttpUrl url = WORKS_URL.newBuilder()
                .addQueryParameter("query.bibliographic", keyword)
                .addQueryParameter("filter", PAPER_TYPES)
                .addQueryParameter("select", "DOI,title,author,published,container-title,abstract")
                .addQueryParameter("rows", Integer.toString(PaperPageSupport.PAGE_SIZE))
                .addQueryParameter("offset", Integer.toString(offset))
                .build();
        Request request = new Request.Builder().url(url).build();
        return parse(httpClient.fetch(PaperSource.CROSSREF, request), offset);
    }

    PaperSearchPageVO parse(String json, int offset) {
        JsonNode message = JacksonUtil.readTree(json).path("message");
        JsonNode records = message.path("items");
        long total = message.path("total-results").asLong(-1);
        if (!records.isArray() || total < 0) {
            throw PaperHttpClient.unavailable(PaperSource.CROSSREF);
        }

        List<PaperHitVO> items = new ArrayList<>();
        for (JsonNode record : records) {
            String title = PaperPageSupport.plainText(record.path("title").path(0).asText(null));
            String doi = record.path("DOI").asText(null);
            if (StringUtils.isAnyBlank(title, doi)) {
                continue;
            }
            String authors = StreamSupport.stream(record.path("author").spliterator(), false)
                    .map(author -> StringUtils.firstNonBlank(author.path("name").asText(null),
                            StringUtils.trim(author.path("given").asText("") + " " + author.path("family").asText(""))))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.joining(", "));
            JsonNode date = record.path("published").path("date-parts").path(0);
            String publicationDate = StreamSupport.stream(date.spliterator(), false)
                    .map(JsonNode::asText)
                    .collect(Collectors.joining("-"));
            items.add(PaperHitVO.builder()
                    .title(title)
                    .authors(StringUtils.trimToNull(authors))
                    .publication(PaperPageSupport.plainText(record.path("container-title").path(0).asText(null)))
                    .publicationDate(StringUtils.trimToNull(publicationDate))
                    .summary(PaperPageSupport.plainText(record.path("abstract").asText(null)))
                    .doi(doi)
                    .url(new HttpUrl.Builder().scheme("https").host("doi.org").addPathSegments(doi).build().toString())
                    .build());
        }
        return new PaperSearchPageVO(items, PaperPageSupport.nextOffset(offset, total));
    }
}
