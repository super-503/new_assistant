package com.zengame.ai.news;

import com.zengame.ai.apiclient.model.NewsTextClient;
import com.zengame.ai.business.news.setting.NewsAiModelSettingService;
import com.zengame.ai.business.news.setting.domain.bo.NewsAiModelConfig;
import com.zengame.ai.business.news.setting.domain.request.NewsAiModelSaveRequest;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.NewsAiModelSettingMapper;
import com.zengame.ai.repository.ai.po.NewsAiModelSettingPO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NewsAiModelSettingTest {

    private static final String SAVED_URL = "https://model.example/v1/chat/completions";
    private static final String NEW_URL = "http://127.0.0.1:11434/v1/chat/completions";

    private final NewsAiModelSettingMapper mapper = mock(NewsAiModelSettingMapper.class);
    private final NewsTextClient client = mock(NewsTextClient.class);
    private final NewsProperties properties = new NewsProperties();
    private final NewsAiModelSettingService settings = new NewsAiModelSettingService(mapper, properties, client);

    @Test
    void requiresBothUrlAndModelAndPrefersTheSavedConfiguration() {
        properties.setModelUrl(SAVED_URL);
        assertThat(settings.get().isConfigured()).isFalse();
        properties.setModel("fallback");
        assertThat(settings.requireConfigured().getModel()).isEqualTo("fallback");

        when(mapper.select()).thenReturn(savedSetting());

        assertThat(settings.requireConfigured().getModel()).isEqualTo("saved-model");
        assertThat(settings.get().isApiKeyConfigured()).isTrue();
    }

    @Test
    void onlyReusesTheSavedKeyWhenTestingTheSameEndpoint() {
        when(mapper.select()).thenReturn(savedSetting());

        settings.verify(request(SAVED_URL));
        settings.verify(request(NEW_URL));
        NewsAiModelSaveRequest withoutKey = request(SAVED_URL);
        withoutKey.setApiKey("");
        settings.verify(withoutKey);

        verify(client).complete(eq(new NewsAiModelConfig(SAVED_URL, "new-model", "saved-key")), eq("Reply with OK."));
        verify(client).complete(eq(new NewsAiModelConfig(NEW_URL, "new-model", null)), eq("Reply with OK."));
        verify(client).complete(eq(new NewsAiModelConfig(SAVED_URL, "new-model", "")), eq("Reply with OK."));
    }

    @Test
    void rejectsEmbeddedCredentialsBeforeCallingTheProvider() {
        assertThatThrownBy(() -> settings.verify(request("https://user:pass@model.example/v1/chat/completions")))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(mapper, client);
    }

    private NewsAiModelSettingPO savedSetting() {
        NewsAiModelSettingPO setting = new NewsAiModelSettingPO();
        setting.setModelUrl(SAVED_URL);
        setting.setModel("saved-model");
        setting.setApiKey("saved-key");
        return setting;
    }

    private NewsAiModelSaveRequest request(String url) {
        NewsAiModelSaveRequest request = new NewsAiModelSaveRequest();
        request.setModelUrl(url);
        request.setModel("new-model");
        return request;
    }
}
