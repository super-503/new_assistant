package com.zengame.ai.news;

import com.zengame.ai.apiclient.feishu.FeishuAppRobotClient;
import com.zengame.ai.apiclient.feishu.FeishuRobotClient;
import com.zengame.ai.business.news.setting.NewsFeishuNotificationSettingService;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuNotificationSaveRequest;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.mapper.NewsFeishuNotificationSettingMapper;
import com.zengame.ai.repository.ai.po.NewsFeishuNotificationSettingPO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class NewsFeishuNotificationSettingTest {

    private final NewsFeishuNotificationSettingMapper mapper = mock(NewsFeishuNotificationSettingMapper.class);
    private final FeishuAppRobotClient client = mock(FeishuAppRobotClient.class);
    private final NewsFeishuNotificationSettingService service = new NewsFeishuNotificationSettingService(
            mapper, client, mock(FeishuRobotClient.class), new NewsProperties());

    @Test
    void keepsTheSavedSecretWhenUpdatingTheSameAppAndReturnsOnlyVisibleFields() {
        when(mapper.selectForUpdate()).thenReturn(savedSetting());

        var saved = service.save(request("cli_saved", null));

        var record = ArgumentCaptor.forClass(NewsFeishuNotificationSettingPO.class);
        verify(mapper).upsert(record.capture());
        assertThat(record.getValue().getAppSecret()).isEqualTo("saved-secret");
        assertThat(record.getValue().getOpenId()).isEqualTo("ou_new");
        assertThat(saved.isConfigured()).isTrue();
        assertThat(JacksonUtil.writeValueAsString(saved)).doesNotContain("appSecret", "saved-secret");
    }

    @Test
    void requiresANewSecretWhenChangingTheAppId() {
        when(mapper.selectForUpdate()).thenReturn(savedSetting());

        assertThatThrownBy(() -> service.save(request("cli_other", " ")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("App Secret");

        verify(mapper).selectForUpdate();
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void sendsATestUsingTheDraftRecipientWithoutSaving() {
        when(mapper.select()).thenReturn(savedSetting());
        var messages = new java.util.ArrayList<String>();
        when(client.sender(new FeishuAppRobotClient.Recipient("cli_saved", "saved-secret", "ou_new")))
                .thenReturn(messages::add);

        service.verify(request("cli_saved", null));

        assertThat(messages).singleElement().asString().contains("测试消息");
        verify(mapper).select();
        verifyNoMoreInteractions(mapper);
    }

    private NewsFeishuNotificationSaveRequest request(String appId, String secret) {
        var request = new NewsFeishuNotificationSaveRequest();
        request.setAppId(appId);
        request.setAppSecret(secret);
        request.setOpenId("ou_new");
        request.setEnabled(true);
        return request;
    }

    private NewsFeishuNotificationSettingPO savedSetting() {
        var setting = new NewsFeishuNotificationSettingPO();
        setting.setAppId("cli_saved");
        setting.setAppSecret("saved-secret");
        setting.setOpenId("ou_old");
        setting.setEnabled(true);
        return setting;
    }
}
