package com.zengame.ai.apiclient.paper;

import com.zengame.ai.business.paper.domain.vo.PaperHitVO;
import com.zengame.ai.business.paper.domain.vo.PaperSearchPageVO;
import com.zengame.ai.infrastructure.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaperClientsTest {

    @Test
    void cnkiParsesThesisMetadataWithoutTreatingTutorAsPublication() {
        String html = "<div class='lplist'><div class='list-item'>"
                + "<p class='tit'><a href='//cdmd.cnki.com.cn/Article/CDMD-123.htm' title='仿真研究'>题名</a></p>"
                + "<p class='nr'>使用<span>仿真</span>方法</p>"
                + "<p class='source'><span><a data-key='李明'>李明</a></span><span>导师：王宁</span>"
                + "<span>某大学学位论文</span><span>2025-06-01</span><span>博士论文</span></p>"
                + "</div></div><input id='hidTotalCount' value='25'>";
        PaperSearchPageVO page = new CnkiClient(null).parse(html, 0);

        assertThat(page.getNextCursor()).isEqualTo("20");
        assertThat(page.getItems()).singleElement().satisfies(hit -> {
            assertThat(hit.getTitle()).isEqualTo("仿真研究");
            assertThat(hit.getAuthors()).isEqualTo("李明");
            assertThat(hit.getPublication()).isEqualTo("某大学学位论文");
            assertThat(hit.getPublicationDate()).isEqualTo("2025-06-01");
            assertThat(hit.getSummary()).isEqualTo("使用仿真方法");
            assertThat(hit.getUrl()).isEqualTo("https://cdmd.cnki.com.cn/Article/CDMD-123.htm");
        });
    }

    @Test
    void cnkiDistinguishesAnEmptySearchFromAnUnexpectedResponse() {
        CnkiClient client = new CnkiClient(null);

        assertThat(client.parse("<div class='no-result'>未检索到文献</div>", 0).getItems()).isEmpty();
        assertThatThrownBy(() -> client.parse("<html>Forbidden</html>", 0)).isInstanceOf(BusinessException.class);
    }

    @Test
    void arxivParsesAtomWithNamespacedMetadata() {
        String xml = "<?xml version='1.0'?><feed xmlns:opensearch='http://a9.com/-/spec/opensearch/1.1/'"
                + " xmlns:arxiv='http://arxiv.org/schemas/atom'>"
                + "<opensearch:totalResults>1</opensearch:totalResults><entry>"
                + "<id>http://arxiv.org/abs/1234.56789v1</id><title>Model evaluation</title>"
                + "<published>2025-06-01T00:00:00Z</published><summary>A &amp; B.</summary>"
                + "<author><name>Chen Li</name></author><author><name>Sam Lee</name></author>"
                + "<arxiv:doi>10.1234/example</arxiv:doi></entry></feed>";
        PaperSearchPageVO page = new ArxivClient(null).parse(xml, 0);

        assertThat(page.getNextCursor()).isNull();
        assertThat(page.getItems()).singleElement().satisfies(hit -> {
            assertThat(hit.getTitle()).isEqualTo("Model evaluation");
            assertThat(hit.getPublicationDate()).isEqualTo("2025-06-01");
            assertThat(hit.getAuthors()).isEqualTo("Chen Li, Sam Lee");
            assertThat(hit.getSummary()).isEqualTo("A & B.");
            assertThat(hit.getDoi()).isEqualTo("10.1234/example");
            assertThat(hit.getUrl()).isEqualTo("https://arxiv.org/abs/1234.56789v1");
        });
    }

    @Test
    void crossrefParsesDoiAndPublicationWithPlainTextAbstract() {
        String json = "{\"message\":{\"total-results\":22,\"items\":[{\"DOI\":\"10.1234/sample(test)\","
                + "\"title\":[\"Sample paper\"],\"author\":[{\"given\":\"Sam\",\"family\":\"Lee\"}],"
                + "\"container-title\":[\"Sample Journal\"],\"published\":{\"date-parts\":[[2025,6]]},"
                + "\"abstract\":\"<jats:p>Abstract &amp; findings</jats:p>\"}]}}";
        PaperSearchPageVO page = new CrossrefClient(null).parse(json, 0);
        PaperHitVO hit = page.getItems().get(0);

        assertThat(page.getNextCursor()).isEqualTo("20");
        assertThat(hit.getAuthors()).isEqualTo("Sam Lee");
        assertThat(hit.getPublication()).isEqualTo("Sample Journal");
        assertThat(hit.getPublicationDate()).isEqualTo("2025-6");
        assertThat(hit.getSummary()).isEqualTo("Abstract & findings");
        assertThat(hit.getUrl()).isEqualTo("https://doi.org/10.1234/sample(test)");
    }

    @Test
    void europePmcParsesArticleAndEndsOnShortPage() {
        String json = "{\"nextCursorMark\":\"next\",\"resultList\":{\"result\":[{"
                + "\"source\":\"MED\",\"id\":\"12345678\",\"title\":\"Sample study\","
                + "\"firstPublicationDate\":\"2025-06-01\",\"authorString\":\"Lee S, Li C.\","
                + "\"journalInfo\":{\"journal\":{\"title\":\"Sample Journal\"}}}]}}";
        PaperSearchPageVO page = new EuropePmcClient(null).parse(json, "*");

        assertThat(page.getNextCursor()).isNull();
        assertThat(page.getItems()).singleElement().satisfies(hit -> {
            assertThat(hit.getPublication()).isEqualTo("Sample Journal");
            assertThat(hit.getPublicationDate()).isEqualTo("2025-06-01");
            assertThat(hit.getUrl()).isEqualTo("https://europepmc.org/article/MED/12345678");
        });
    }

    @Test
    void invalidOrExcessiveOffsetIsRejectedBeforeMakingNetworkRequests() {
        CrossrefClient client = new CrossrefClient(null);

        assertThatThrownBy(() -> client.search("sample", "1000")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> client.search("sample", "-20")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> client.search("sample", "21")).isInstanceOf(BusinessException.class);
    }
}
