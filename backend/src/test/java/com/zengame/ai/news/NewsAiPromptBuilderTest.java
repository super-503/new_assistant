package com.zengame.ai.news;

import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldSource;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.processing.NewsAiPromptBuilder;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.zengame.ai.news.NewsAiFieldFixtures.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据加工提示词组装：参与生成的字段筛选、来源内容、字段要求。
 */
class NewsAiPromptBuilderTest {

    private final NewsAiPromptBuilder builder = new NewsAiPromptBuilder();

    @Test
    void selectApplicableSkipsDisabledEmptyOptionsAndArticleOnlyForVideo() {
        NewsAiFieldPO title = field("title", NewsAiFieldSource.TITLE_BODY, NewsAiFieldOutputType.TEXT, Collections.emptyList());
        NewsAiFieldPO summary = field("summary", NewsAiFieldSource.BODY, NewsAiFieldOutputType.TEXT, Collections.emptyList());
        summary.setArticleOnly(true);
        NewsAiFieldPO topic = field("topic", NewsAiFieldSource.TITLE_BODY, NewsAiFieldOutputType.MULTI, Collections.emptyList());
        NewsAiFieldPO level = field("level", NewsAiFieldSource.BODY, NewsAiFieldOutputType.SINGLE, Arrays.asList("入门", "进阶"));
        NewsAiFieldPO score = field("score", NewsAiFieldSource.BODY, NewsAiFieldOutputType.NUMBER, Collections.emptyList());
        score.setEnabled(false);
        List<NewsAiFieldPO> fields = Arrays.asList(title, summary, topic, level, score);

        assertEquals(Arrays.asList("title", "summary", "level"), keys(builder.selectApplicable(fields, NewsContentType.ARTICLE)));
        assertEquals(Arrays.asList("title", "level"), keys(builder.selectApplicable(fields, NewsContentType.VIDEO)));
    }

    @Test
    void buildWritesContentOnceAndEveryFieldRequirement() {
        NewsItemPO item = new NewsItemPO();
        item.setOriginTitle("  Unity 6 发布  ");
        item.setBodyHtml("<p>第一段<b>加粗</b></p><script>ignored()</script><p>第二段</p>");
        NewsAiFieldPO summary = field("summary", NewsAiFieldSource.BODY, NewsAiFieldOutputType.TEXT, Collections.emptyList());
        summary.setMultiline(true);
        NewsAiFieldPO jobFamily = field("job_family", NewsAiFieldSource.TITLE_BODY, NewsAiFieldOutputType.MULTI,
                Arrays.asList("技术美术", "客户端开发"));
        NewsAiFieldPO score = field("score", NewsAiFieldSource.TITLE, NewsAiFieldOutputType.NUMBER, Collections.emptyList());

        String prompt = builder.build(item, Arrays.asList(summary, jobFamily, score));

        assertTrue(prompt.contains("## 资讯标题\nUnity 6 发布\n"));
        assertTrue(prompt.contains("## 资讯正文\n第一段加粗 第二段\n"));
        assertFalse(prompt.contains("<p>"));
        assertFalse(prompt.contains("ignored()"));
        assertTrue(prompt.contains("1. summary（summary名称）"));
        assertTrue(prompt.contains("多行文本"));
        assertTrue(prompt.contains("summary的提示词"));
        assertTrue(prompt.contains("2. job_family（job_family名称）"));
        assertTrue(prompt.contains("可选项：[\"技术美术\",\"客户端开发\"]"));
        assertTrue(prompt.contains("多选，输出字符串数组"));
        assertTrue(prompt.contains("3. score（score名称）"));
        assertTrue(prompt.contains("依据：资讯标题\n"));
        assertTrue(prompt.contains("数字，输出一个 JSON 数字"));
        assertTrue(prompt.contains("必须包含且只包含：summary、job_family、score。"));
    }

    @Test
    void buildOmitsTitleWhenOnlyBodyIsUsedAndTruncatesBody() {
        NewsItemPO item = new NewsItemPO();
        item.setOriginTitle("不应出现的标题");
        item.setBodyHtml("<div>" + StringUtils.repeat("字", NewsAiPromptBuilder.BODY_MAX_CHARS + 100) + "</div>");
        NewsAiFieldPO summary = field("summary", NewsAiFieldSource.BODY, NewsAiFieldOutputType.TEXT, Collections.emptyList());

        String prompt = builder.build(item, Collections.singletonList(summary));

        assertFalse(prompt.contains("## 资讯标题"));
        assertFalse(prompt.contains("不应出现的标题"));
        assertTrue(prompt.contains(StringUtils.repeat("字", NewsAiPromptBuilder.BODY_MAX_CHARS)));
        assertFalse(prompt.contains(StringUtils.repeat("字", NewsAiPromptBuilder.BODY_MAX_CHARS + 1)));
    }

    @Test
    void buildMarksMissingTitleAndBody() {
        NewsItemPO item = new NewsItemPO();
        NewsAiFieldPO title = field("title", NewsAiFieldSource.TITLE_BODY, NewsAiFieldOutputType.TEXT, Collections.emptyList());

        String prompt = builder.build(item, Collections.singletonList(title));

        assertTrue(prompt.contains("## 资讯标题\n（无标题）"));
        assertTrue(prompt.contains("## 资讯正文\n（无正文）"));
    }

    private List<String> keys(List<NewsAiFieldPO> fields) {
        return fields.stream().map(NewsAiFieldPO::getFieldKey).collect(Collectors.toList());
    }
}
