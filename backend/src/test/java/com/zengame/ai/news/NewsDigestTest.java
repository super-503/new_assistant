package com.zengame.ai.news;

import com.zengame.ai.apiclient.feishu.FeishuAppRobotClient;
import com.zengame.ai.apiclient.feishu.FeishuRobotClient;
import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import com.zengame.ai.business.news.library.NewsDigestService;
import com.zengame.ai.business.news.setting.NewsFeishuNotificationSettingService;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.NewsFeishuNotificationSettingMapper;
import com.zengame.ai.repository.ai.mapper.NewsItemMapper;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import com.zengame.ai.repository.ai.po.NewsFeishuNotificationSettingPO;
import com.zengame.ai.task.NewsDigestTask;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NewsDigestTest {

    private static final String WEBHOOK = "https://example.com/personal-bot";
    private static final String VIDEO_URL = "https://www.bilibili.com/video/BV1xx411c7mD";
    private static final String TOKEN_URL = "https://accounts.feishu.cn/oauth/v3/token";
    private static final String MESSAGE_URL = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=open_id";

    private final NewsItemMapper items = mock(NewsItemMapper.class);
    private final NewsFeishuNotificationSettingMapper settings = mock(NewsFeishuNotificationSettingMapper.class);
    private final NewsProperties properties = new NewsProperties();
    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer feishu = MockRestServiceServer.bindTo(restTemplate).build();
    private final FeishuRobotClient webhookClient = new FeishuRobotClient(restTemplate, properties);
    private final NewsDigestService service = new NewsDigestService(items);
    private final NewsDigestTask task = new NewsDigestTask(new NewsFeishuNotificationSettingService(settings,
            new FeishuAppRobotClient(restTemplate), webhookClient, properties), service);

    @Test
    void sendsApprovedBilibiliVideoAndRecordsSuccessfulDelivery() {
        properties.setDigestFeishuWebhook(WEBHOOK);
        when(items.selectUnsentDigestItemsForUpdate(eq(NewsItemStatus.APPROVED), anyInt()))
                .thenReturn(List.of(video()))
                .thenReturn(List.of());
        feishu.expect(requestTo(WEBHOOK))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.msg_type").value("text"))
                .andExpect(jsonPath("$.content.text").value("资讯日报\n\n• B 站视频 · UP 主\n" + VIDEO_URL))
                .andRespond(withSuccess("{\"code\":0}", MediaType.APPLICATION_JSON));

        task.sendDigest();

        feishu.verify();
        verify(items).markDigestSent(List.of(42L));
    }

    @Test
    void leavesItemsPendingWhenFeishuRejectsTheBatch() {
        when(items.selectUnsentDigestItemsForUpdate(eq(NewsItemStatus.APPROVED), anyInt())).thenReturn(List.of(video()));
        feishu.expect(requestTo(WEBHOOK)).andRespond(withSuccess("{\"code\":19024}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.sendBatch(message -> webhookClient.sendRobotMessage(WEBHOOK, message)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("19024");

        feishu.verify();
        verify(items, never()).markDigestSent(anyList());
    }

    @Test
    void recordsOnlyItemsIncludedInTheMessage() {
        NewsItemPO first = longItem(1L);
        NewsItemPO second = longItem(2L);
        NewsItemPO third = longItem(3L);
        when(items.selectUnsentDigestItemsForUpdate(eq(NewsItemStatus.APPROVED), anyInt()))
                .thenReturn(List.of(first, second, third));
        feishu.expect(requestTo(WEBHOOK))
                .andExpect(jsonPath("$.content.text").value(containsString(second.getCanonicalUrl())))
                .andRespond(withSuccess("{\"code\":0}", MediaType.APPLICATION_JSON));

        assertThat(service.sendBatch(message -> webhookClient.sendRobotMessage(WEBHOOK, message))).isEqualTo(2);

        feishu.verify();
        verify(items).markDigestSent(List.of(1L, 2L));
    }

    @Test
    void skipsDigestWithoutAPersonalWebhook() {
        properties.setFeishuWebhook("https://example.com/alert-bot");

        task.sendDigest();

        verifyNoInteractions(items);
    }

    @Test
    void sendsTheDigestToTheSavedAppRecipientAndReusesTheTokenAcrossBatches() {
        properties.setDigestFeishuWebhook(WEBHOOK);
        when(settings.select()).thenReturn(appSetting(true));
        when(items.selectUnsentDigestItemsForUpdate(eq(NewsItemStatus.APPROVED), anyInt()))
                .thenReturn(List.of(video()))
                .thenReturn(List.of(longItem(43L)))
                .thenReturn(List.of());
        expectToken();
        expectAppMessage(0);
        expectAppMessage(0);

        task.sendDigest();

        feishu.verify();
        verify(items).markDigestSent(List.of(42L));
        verify(items).markDigestSent(List.of(43L));
    }

    @Test
    void leavesItemsPendingWhenTheAppCannotSendToTheRecipient() {
        when(settings.select()).thenReturn(appSetting(true));
        when(items.selectUnsentDigestItemsForUpdate(eq(NewsItemStatus.APPROVED), anyInt())).thenReturn(List.of(video()));
        expectToken();
        expectAppMessage(230013);

        assertThatThrownBy(task::sendDigest).isInstanceOf(BusinessException.class).hasMessageContaining("230013");

        feishu.verify();
        verify(items, never()).markDigestSent(anyList());
    }

    @Test
    void disabledAppConfigurationPausesDigestDelivery() {
        properties.setDigestFeishuWebhook(WEBHOOK);
        when(settings.select()).thenReturn(appSetting(false));

        task.sendDigest();

        verifyNoInteractions(items);
    }

    private void expectToken() {
        feishu.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.grant_type").value("client_credentials"))
                .andExpect(jsonPath("$.client_id").value("cli_demo"))
                .andExpect(jsonPath("$.client_secret").value("test-secret"))
                .andRespond(withSuccess("{\"code\":0,\"access_token\":\"test-token\",\"expires_in\":7200}",
                        MediaType.APPLICATION_JSON));
    }

    private void expectAppMessage(int code) {
        feishu.expect(requestTo(MESSAGE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(jsonPath("$.receive_id").value("ou_demo"))
                .andExpect(jsonPath("$.msg_type").value("text"))
                .andExpect(jsonPath("$.content").value(containsString(VIDEO_URL)))
                .andRespond(withSuccess("{\"code\":" + code + "}", MediaType.APPLICATION_JSON));
    }

    private NewsFeishuNotificationSettingPO appSetting(boolean enabled) {
        NewsFeishuNotificationSettingPO setting = new NewsFeishuNotificationSettingPO();
        setting.setAppId("cli_demo");
        setting.setAppSecret("test-secret");
        setting.setOpenId("ou_demo");
        setting.setEnabled(enabled);
        return setting;
    }

    private NewsItemPO video() {
        NewsItemPO item = new NewsItemPO();
        item.setId(42L);
        item.setOriginTitle("B 站视频");
        item.setAuthor("UP 主");
        item.setCanonicalUrl(VIDEO_URL);
        return item;
    }

    private NewsItemPO longItem(long id) {
        NewsItemPO item = video();
        item.setId(id);
        item.setOriginTitle("长".repeat(512));
        item.setAuthor("长".repeat(128));
        item.setCanonicalUrl(VIDEO_URL + "?id=" + id + "&" + "a".repeat(600));
        return item;
    }
}
