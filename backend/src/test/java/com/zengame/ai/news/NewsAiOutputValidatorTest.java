package com.zengame.ai.news;

import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldSource;
import com.zengame.ai.business.news.processing.NewsAiOutputValidator;
import com.zengame.ai.business.news.processing.domain.bo.NewsAiGeneration;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.zengame.ai.news.NewsAiFieldFixtures.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 数据加工结果校验：按输出类型和选项逐个字段校验，非法的字段记为未生成。
 */
class NewsAiOutputValidatorTest {

    private final NewsAiOutputValidator validator = new NewsAiOutputValidator();

    private final List<NewsAiFieldPO> fields = Arrays.asList(
            field("title", NewsAiFieldSource.TITLE_BODY, NewsAiFieldOutputType.TEXT, Collections.emptyList()),
            field("score", NewsAiFieldSource.BODY, NewsAiFieldOutputType.NUMBER, Collections.emptyList()),
            field("level", NewsAiFieldSource.BODY, NewsAiFieldOutputType.SINGLE, Arrays.asList("入门", "进阶")),
            field("topic", NewsAiFieldSource.TITLE_BODY, NewsAiFieldOutputType.MULTI, Arrays.asList("渲染", "AI", "引擎")));

    @Test
    void validValuesAreNormalizedByOutputType() {
        String content = "```json\n{\"title\":\"  Unity 6 发布 \",\"score\":3.5,\"level\":\"进阶\",\"topic\":[\"AI\",\"渲染\",\"AI\"],\"extra\":1}\n```";

        NewsAiGeneration generation = validator.validate(fields, content);

        assertEquals("{\"title\":\"Unity 6 发布\",\"score\":3.5,\"level\":\"进阶\",\"topic\":[\"AI\",\"渲染\"]}",
                generation.getValues().toString());
        assertEquals(Collections.emptyList(), generation.getFailedKeys());
    }

    @Test
    void emptyArrayIsValidForMulti() {
        NewsAiGeneration generation = validator.validate(fields,
                "{\"title\":\"标题\",\"score\":1,\"level\":\"入门\",\"topic\":[]}");

        assertEquals("[]", generation.getValues().get("topic").toString());
        assertEquals(Collections.emptyList(), generation.getFailedKeys());
    }

    @Test
    void missingWrongTypeAndOutOfOptionValuesFail() {
        NewsAiGeneration generation = validator.validate(fields,
                "{\"title\":123,\"score\":\"3\",\"level\":\"高级\",\"topic\":[\"AI\",\"游戏\"]}");

        assertEquals("{}", generation.getValues().toString());
        assertEquals(Arrays.asList("title", "score", "level", "topic"), generation.getFailedKeys());
    }

    @Test
    void blankTextIsEmptyValueForOptionalField() {
        NewsAiFieldPO product = field("product", NewsAiFieldSource.TITLE_BODY, NewsAiFieldOutputType.TEXT,
                Collections.emptyList());

        NewsAiGeneration generation = validator.validate(Collections.singletonList(product), "{\"product\":\"  \"}");

        assertEquals("{\"product\":\"\"}", generation.getValues().toString());
        assertEquals(Collections.emptyList(), generation.getFailedKeys());
    }

    @Test
    void blankTextIsMissingForRequiredField() {
        NewsAiFieldPO product = field("product", NewsAiFieldSource.TITLE_BODY, NewsAiFieldOutputType.TEXT,
                Collections.emptyList());
        product.setRequired(true);

        NewsAiGeneration generation = validator.validate(Collections.singletonList(product), "{\"product\":\"\"}");

        assertEquals("{}", generation.getValues().toString());
        assertEquals(Collections.singletonList("product"), generation.getFailedKeys());
    }

    @Test
    void absentAndNullValuesFail() {
        NewsAiGeneration generation = validator.validate(fields, "{\"title\":\"标题\",\"score\":null,\"topic\":\"AI\"}");

        assertEquals("{\"title\":\"标题\"}", generation.getValues().toString());
        assertEquals(Arrays.asList("score", "level", "topic"), generation.getFailedKeys());
    }

    @Test
    void nonObjectOutputIsRejected() {
        assertThrows(IllegalStateException.class, () -> validator.validate(fields, "无法生成"));
        assertThrows(RuntimeException.class, () -> validator.validate(fields, "{not json}"));
    }
}
