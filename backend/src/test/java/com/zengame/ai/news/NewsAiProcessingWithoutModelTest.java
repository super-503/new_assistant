package com.zengame.ai.news;

import com.zengame.ai.apiclient.model.NewsTextClient;
import com.zengame.ai.business.news.NewsAiFieldCatalog;
import com.zengame.ai.business.news.processing.NewsAiModelInvoker;
import com.zengame.ai.business.news.processing.NewsAiOutputValidator;
import com.zengame.ai.business.news.processing.NewsAiProcessService;
import com.zengame.ai.business.news.processing.NewsAiPromptBuilder;
import com.zengame.ai.business.news.setting.NewsAiModelSettingService;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.NewsAiFieldMapper;
import com.zengame.ai.repository.ai.mapper.NewsAiModelSettingMapper;
import com.zengame.ai.repository.ai.mapper.NewsItemLogMapper;
import com.zengame.ai.repository.ai.mapper.NewsItemMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class NewsAiProcessingWithoutModelTest {

    private final NewsItemMapper items = mock(NewsItemMapper.class);
    private final NewsItemLogMapper logs = mock(NewsItemLogMapper.class);
    private final NewsAiFieldMapper fields = mock(NewsAiFieldMapper.class);
    private final NewsAiModelInvoker invoker = mock(NewsAiModelInvoker.class);
    private final NewsProperties properties = new NewsProperties();
    private final NewsAiModelSettingService settings = new NewsAiModelSettingService(
            mock(NewsAiModelSettingMapper.class), properties, mock(NewsTextClient.class));
    private final NewsAiProcessService processing = new NewsAiProcessService(
            items, logs, fields, new NewsAiFieldCatalog(properties, fields), settings,
            new NewsAiPromptBuilder(), new NewsAiOutputValidator(), invoker, mock(TransactionTemplate.class));

    @Test
    void movesNewItemsToReviewWithoutGeneratingFieldsOrLoggingAnAiFailure() {
        List<Long> ids = List.of(17L, 18L);

        processing.processItems(ids);

        verify(items).markPendingWithoutProcessing(ids);
        verifyNoInteractions(fields, invoker, logs);
    }

    @Test
    void rejectsManualGenerationWhenNoModelIsConfigured() {
        assertThatThrownBy(() -> processing.reprocess(17L, "admin")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> processing.generateField(17L, "title", "admin"))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(items, fields, invoker, logs);
    }
}
