package com.zengame.ai.news;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.zengame.ai.apiclient.feishu.FeishuAppRobotClient;
import com.zengame.ai.apiclient.feishu.FeishuRobotClient;
import com.zengame.ai.business.news.NewsSettingController;
import com.zengame.ai.business.news.library.NewsFeishuDigestService;
import com.zengame.ai.business.news.setting.NewsFeishuNotificationSettingService;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuCardSettings;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuChannelConfig;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuChannelSaveRequest;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuNotificationSaveRequest;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.exception.WebExceptionAdapter;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.mapper.NewsFeishuNotificationSettingMapper;
import com.zengame.ai.repository.ai.po.NewsFeishuAuthorizedUserPO;
import com.zengame.ai.repository.ai.po.NewsFeishuChannelConfigPO;
import com.zengame.ai.repository.ai.po.NewsFeishuNotificationSettingPO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NewsFeishuNotificationSettingTest {

    private static final String CHANNEL = "/ai/api/web/v1/news/setting/notification-channel/feishu";
    private final NewsFeishuNotificationSettingMapper mapper = mock(NewsFeishuNotificationSettingMapper.class);
    private final FeishuAppRobotClient client = mock(FeishuAppRobotClient.class);
    private final NewsFeishuNotificationSettingService service = new NewsFeishuNotificationSettingService(
            mapper, client, mock(FeishuRobotClient.class), new NewsProperties(), mock(NewsFeishuDigestService.class));

    @Test
    void savesTheBrowserPayloadAndLoadsTheRecipientWithoutAuthorizingAgain() throws Exception {
        var initial = new NewsFeishuChannelConfigPO();
        var persisted = new NewsFeishuChannelConfigPO();
        when(mapper.selectChannelForUpdate()).thenReturn(initial);
        when(mapper.selectChannel()).thenReturn(persisted);
        when(mapper.select()).thenReturn(savedSetting());
        var user = authorizedUser("cli_saved", "ou_old");
        user.setName("Alice");
        user.setAvatarUrl("https://example.com/avatar.png");
        when(mapper.selectAuthorizedUser("cli_saved", "ou_old")).thenReturn(user);
        when(mapper.updateChannel(anyString())).thenAnswer(invocation -> {
            persisted.setConfigJson(invocation.getArgument(0));
            persisted.setRevision(1);
            return 1;
        });
        var objectMapper = Jackson2ObjectMapperBuilder.json()
                .modulesToInstall(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES)).build();
        var mvc = MockMvcBuilders.standaloneSetup(new NewsSettingController(null, null, service, null, null))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new WebExceptionAdapter()).build();

        mvc.perform(put(CHANNEL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"revision\":0,\"apps\":[{\"appId\":\"cli_saved\",\"appSecret\":null}],"
                                + "\"recipients\":[{\"appId\":\"cli_saved\",\"openId\":\"ou_old\",\"name\":\"Ignored name\"}],"
                                + "\"card\":{\"title\":\"Digest\",\"color\":\"purple\",\"showAuthor\":false,"
                                + "\"template\":{\"id\":\"AAexample\",\"variablesJson\":\"{\\\"rows\\\":\\\"${items}\\\"}\","
                                + "\"variableTypes\":{\"rows\":\"array\"}}},"
                                + "\"content\":{\"deliveryChannels\":[\"FEISHU\"],\"contentTypes\":[\"VIDEO\"],"
                                + "\"reviewDays\":7,\"fields\":[\"ai:summary\"]},"
                                + "\"enabled\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.revision").value(1));
        mvc.perform(get(CHANNEL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipients[0].openId").value("ou_old"))
                .andExpect(jsonPath("$.data.recipients[0].name").value("Alice"))
                .andExpect(jsonPath("$.data.recipients[0].avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.data.card.color").value("purple"))
                .andExpect(jsonPath("$.data.card.template.id").value("AAexample"))
                .andExpect(jsonPath("$.data.card.template.variableTypes.rows").value("array"))
                .andExpect(jsonPath("$.data.content.deliveryChannels[0]").value("FEISHU"))
                .andExpect(jsonPath("$.data.content.contentTypes[0]").value("VIDEO"))
                .andExpect(jsonPath("$.data.content.reviewDays").value(7))
                .andExpect(jsonPath("$.data.content.fields[0]").value("ai:summary"))
                .andExpect(jsonPath("$.data.apps[0].appSecret").doesNotExist());
        verifyNoInteractions(client);
    }

    @Test
    void savesMultipleAppsAndRecipientsAndCarriesOverTheExistingSecret() {
        var row = new NewsFeishuChannelConfigPO();
        when(mapper.selectChannelForUpdate()).thenReturn(row);
        when(mapper.select()).thenReturn(savedSetting());
        when(mapper.selectAuthorizedUser("cli_saved", "ou_same")).thenReturn(authorizedUser("cli_saved", "ou_same"));
        when(mapper.selectAuthorizedUser("cli_second", "ou_same")).thenReturn(authorizedUser("cli_second", "ou_same"));
        var first = new NewsFeishuChannelSaveRequest.App();
        first.setAppId("cli_saved");
        var second = new NewsFeishuChannelSaveRequest.App();
        second.setAppId("cli_second");
        second.setAppSecret("second-secret");
        var firstRecipient = new NewsFeishuChannelSaveRequest.Recipient();
        firstRecipient.setAppId("cli_saved");
        firstRecipient.setOpenId("ou_same");
        var secondRecipient = new NewsFeishuChannelSaveRequest.Recipient();
        secondRecipient.setAppId("cli_second");
        secondRecipient.setOpenId("ou_same");
        var request = new NewsFeishuChannelSaveRequest();
        request.setRevision(0L);
        request.setApps(List.of(first, second));
        request.setRecipients(List.of(firstRecipient, secondRecipient));
        request.setCard(new NewsFeishuCardSettings());
        request.setEnabled(true);

        var visible = service.saveChannel(request);
        var json = ArgumentCaptor.forClass(String.class);
        verify(mapper).updateChannel(json.capture());
        var stored = JacksonUtil.readValue(json.getValue(), NewsFeishuChannelConfig.class);
        assertThat(stored.getApps()).extracting(NewsFeishuChannelConfig.App::getAppSecret)
                .containsExactly("saved-secret", "second-secret");
        assertThat(visible.getRecipients()).hasSize(2);
        assertThat(visible.getRevision()).isEqualTo(1);
        assertThat(JacksonUtil.writeValueAsString(visible)).doesNotContain("appSecret", "saved-secret", "second-secret");
    }

    @Test
    void acceptsPreviouslySavedRecipientsButRejectsAnUnknownRecipientForThisApp() {
        when(mapper.selectChannelForUpdate()).thenReturn(new NewsFeishuChannelConfigPO());
        when(mapper.select()).thenReturn(savedSetting());
        var app = new NewsFeishuChannelSaveRequest.App();
        app.setAppId("cli_saved");
        var recipient = new NewsFeishuChannelSaveRequest.Recipient();
        recipient.setAppId("cli_saved");
        recipient.setOpenId("ou_other");
        var request = new NewsFeishuChannelSaveRequest();
        request.setRevision(0L);
        request.setApps(List.of(app));
        request.setRecipients(List.of(recipient));
        request.setCard(new NewsFeishuCardSettings());

        assertThatThrownBy(() -> service.saveChannel(request)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("已授权人员");
        verify(mapper, never()).updateChannel(anyString());

        recipient.setOpenId("ou_old");
        assertThat(service.saveChannel(request).getRecipients()).singleElement()
                .satisfies(saved -> assertThat(saved.getOpenId()).isEqualTo("ou_old"));
    }

    private NewsFeishuAuthorizedUserPO authorizedUser(String appId, String openId) {
        var user = new NewsFeishuAuthorizedUserPO();
        user.setAppId(appId);
        user.setOpenId(openId);
        return user;
    }

    @Test
    void rejectsAnOverlappingSaveFromAnotherTab() {
        var row = new NewsFeishuChannelConfigPO();
        row.setRevision(2);
        when(mapper.selectChannelForUpdate()).thenReturn(row);
        var request = new NewsFeishuChannelSaveRequest();
        request.setRevision(1L);

        assertThatThrownBy(() -> service.saveChannel(request))
                .isInstanceOf(BusinessException.class).hasMessageContaining("刷新页面");
        verify(mapper).selectChannelForUpdate();
        verifyNoMoreInteractions(mapper);
    }

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

    @Test
    void savesRecipientProfileAndPreservesItOnlyForTheSameAppAndRecipient() {
        var old = savedSetting();
        old.setRecipientName("原接收人");
        old.setRecipientAvatarUrl("https://example.com/old.png");
        when(mapper.selectForUpdate()).thenReturn(old);

        var unchanged = request("cli_saved", null);
        unchanged.setOpenId("ou_old");
        assertThat(service.save(unchanged).getRecipientName()).isEqualTo("原接收人");

        var replacement = request("cli_saved", null);
        assertThat(service.save(replacement).getRecipientAvatarUrl()).isNull();

        replacement.setRecipientName("新接收人");
        replacement.setRecipientAvatarUrl("https://example.com/new.png");
        var saved = service.save(replacement);
        assertThat(saved.getRecipientName()).isEqualTo("新接收人");
        assertThat(saved.getRecipientAvatarUrl()).isEqualTo("https://example.com/new.png");
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
