package com.zengame.ai.news;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldKind;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldSource;
import com.zengame.ai.business.news.review.cover.NewsItemViewConvert;
import com.zengame.ai.business.news.review.domain.bo.NewsItemBO;
import com.zengame.ai.business.news.review.domain.vo.NewsFieldOptionVO;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.zengame.ai.news.NewsAiFieldFixtures.field;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 资讯标签取值：岗位分类选中全部选项时合并成一个「全部」。
 */
class NewsTagValuesTest {

    private static final List<String> JOB_FAMILIES = Arrays.asList("产品族", "技术族", "市场族");

    private final NewsItemViewConvert convert = NewsItemViewConvert.INSTANCE;

    @Test
    void allJobFamiliesShowAsSingleAllTag() {
        List<NewsFieldOptionVO> values = convert.tagValues(jobFamilyField(),
                itemWithValues("{\"job_family\":[\"产品族\",\"技术族\",\"市场族\"]}"));

        assertEquals(1, values.size());
        assertEquals("全部", values.get(0).getValue());
        assertEquals(null, values.get(0).getColorIndex());
    }

    @Test
    void partialJobFamiliesStayAsIs() {
        List<NewsFieldOptionVO> values = convert.tagValues(jobFamilyField(),
                itemWithValues("{\"job_family\":[\"产品族\",\"技术族\"]}"));

        assertEquals(Arrays.asList("产品族", "技术族"), namesOf(values));
    }

    @Test
    void otherTagFieldKeepsEveryValue() {
        NewsAiFieldPO topic = field("topic", NewsAiFieldSource.TITLE_BODY, NewsAiFieldOutputType.MULTI, JOB_FAMILIES);
        topic.setShowAsTag(true);

        List<NewsFieldOptionVO> values = convert.tagValues(topic,
                itemWithValues("{\"topic\":[\"产品族\",\"技术族\",\"市场族\"]}"));

        assertEquals(JOB_FAMILIES, namesOf(values));
    }

    private NewsAiFieldPO jobFamilyField() {
        NewsAiFieldPO field = field("job_family", NewsAiFieldSource.TITLE_BODY, NewsAiFieldOutputType.MULTI, JOB_FAMILIES);
        field.setKind(NewsAiFieldKind.BUILTIN);
        return field;
    }

    private NewsItemBO itemWithValues(String aiValuesJson) {
        NewsItemBO item = new NewsItemBO();
        item.setAiValues((ObjectNode) JacksonUtil.readTree(aiValuesJson));
        return item;
    }

    private List<String> namesOf(List<NewsFieldOptionVO> values) {
        return values.stream().map(NewsFieldOptionVO::getValue).collect(Collectors.toList());
    }
}
