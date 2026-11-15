package com.zengame.ai.news;

import com.zengame.ai.apiclient.feishu.FeishuAppRobotClient;
import com.zengame.ai.apiclient.feishu.FeishuRobotClient;
import com.zengame.ai.business.news.NewsAiFieldCatalog;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import com.zengame.ai.business.news.library.NewsDigestService;
import com.zengame.ai.business.news.library.NewsFeishuDigestService;
import com.zengame.ai.business.news.setting.NewsFeishuNotificationSettingService;
import com.zengame.ai.business.news.setting.domain.bo.NewsDigestContentSettings;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuCardSettings;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuCardTemplate;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuDigestDelivery;
import com.zengame.ai.business.news.setting.domain.bo.NewsPushPlanDefinition;
import com.zengame.ai.business.news.setting.domain.enums.NewsPushPlanBatchResult;
import com.zengame.ai.business.news.setting.domain.enums.NewsPushPlanCard;
import com.zengame.ai.business.news.setting.domain.request.NewsPushPlanPreviewRequest;
import com.zengame.ai.business.news.setting.domain.enums.NewsFeishuCardColor;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuCardTestRequest;
import com.zengame.ai.repository.ai.mapper.NewsFeishuNotificationSettingMapper;
import com.zengame.ai.repository.ai.mapper.NewsPushPlanMapper;
import com.zengame.ai.repository.ai.mapper.NewsFeishuDigestDeliveryMapper;
import com.zengame.ai.repository.ai.mapper.NewsItemMapper;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import com.zengame.ai.repository.ai.po.NewsFeishuChannelConfigPO;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import com.zengame.ai.repository.ai.po.NewsFeishuNotificationSettingPO;
import com.zengame.ai.task.NewsDigestTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
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
            new FeishuAppRobotClient(restTemplate), webhookClient, properties, mock(NewsFeishuDigestService.class)), service,
            mock(NewsFeishuDigestService.class), mock(NewsPushPlanMapper.class));

    @Test
    void onlySchedulesFeishuWhenSelectedAndDefaultsToFeishuForOlderContentSettings() {
        var channel = new NewsFeishuChannelConfigPO();
        var cards = mock(NewsFeishuDigestService.class);
        var notification = new NewsFeishuNotificationSettingService(settings, new FeishuAppRobotClient(restTemplate),
                webhookClient, properties, cards);
        var digestTask = new NewsDigestTask(notification, service, cards, mock(NewsPushPlanMapper.class));
        when(settings.selectChannel()).thenReturn(channel);
        properties.setDigestFeishuWebhook(WEBHOOK);
        String config = "{\"enabled\":true,\"apps\":[{\"appId\":\"cli_test\",\"appSecret\":\"test-secret\"}],"
                + "\"recipients\":[{\"appId\":\"cli_test\",\"openId\":\"ou_test\"}],\"content\":%s}";
        channel.setConfigJson(String.format(config, "{\"deliveryChannels\":[]}"));

        digestTask.sendDigest();

        verifyNoInteractions(cards, items);
        channel.setConfigJson(String.format(config, "{\"channelCodes\":[]}"));
        digestTask.sendDigest();

        var delivery = ArgumentCaptor.forClass(NewsFeishuDigestDelivery.class);
        verify(cards).sendBatch(delivery.capture());
        assertThat(delivery.getValue().getTargets()).singleElement()
                .satisfies(target -> assertThat(target.getOpenId()).isEqualTo("ou_test"));
        verifyNoInteractions(items);
    }

    @Test
    void recordsTheSameArticleIndependentlyForDifferentPlansWithoutMarkingTheLegacyDigest() {
        var receipts = mock(NewsFeishuDigestDeliveryMapper.class);
        var cards = new NewsFeishuDigestService(items, receipts, mock(NewsAiFieldCatalog.class));
        var scope = new NewsDigestContentSettings();
        var article = video();
        when(items.selectPushPlanRecipientItemsForUpdate(eq(12L), eq("cli_one"), eq("ou_one"),
                eq(NewsItemStatus.APPROVED), eq(scope), anyInt()))
                .thenReturn(List.of(article));
        when(items.selectPushPlanRecipientItemsForUpdate(eq(34L), eq("cli_one"), eq("ou_one"),
                eq(NewsItemStatus.APPROVED), eq(scope), anyInt()))
                .thenReturn(List.of(article));
        var sent = new ArrayList<String>();
        var delivery = new NewsFeishuDigestDelivery(new NewsFeishuCardSettings(), scope, List.of(
                new NewsFeishuDigestDelivery.Target("cli_one", "ou_one", sent::add)));

        assertThat(cards.sendPlanBatch(12, delivery)).isTrue();
        assertThat(cards.sendPlanBatch(34, delivery)).isTrue();
        assertThat(sent).hasSize(2);
        verify(receipts).markPlanDelivered(12, "cli_one", "ou_one", List.of(article.getId()));
        verify(receipts).completePlanItems(12, delivery.getTargets());
        verify(receipts).completePlanItems(34, delivery.getTargets());
        verify(items, never()).markDigestSent(anyList());
    }

    @Test
    void continuesHealthyRecipientsWithNewItemsWhileAnotherRecipientWaitsForRetry() {
        var receipts = mock(NewsFeishuDigestDeliveryMapper.class);
        var cards = new NewsFeishuDigestService(items, receipts, mock(NewsAiFieldCatalog.class));
        var scope = new NewsDigestContentSettings();
        var first = video();
        var next = new NewsItemPO();
        next.setId(first.getId() + 1);
        next.setOriginTitle("Next article");
        when(items.selectPushPlanRecipientItemsForUpdate(12, "cli_one", "ou_first", NewsItemStatus.APPROVED, scope, 20))
                .thenReturn(List.of(first), List.of(next));
        when(items.selectPushPlanRecipientItemsForUpdate(12, "cli_one", "ou_failed", NewsItemStatus.APPROVED, scope, 20))
                .thenReturn(List.of(first));
        var sent = new ArrayList<String>();
        var delivery = new NewsFeishuDigestDelivery(new NewsFeishuCardSettings(), scope, List.of(
                new NewsFeishuDigestDelivery.Target("cli_one", "ou_failed", content -> {
                    throw new BusinessException("recipient unavailable");
                }), new NewsFeishuDigestDelivery.Target("cli_one", "ou_first", sent::add)));

        assertThat(cards.sendPlanBatchResult(12, delivery)).isEqualTo(NewsPushPlanBatchResult.RETRY);
        assertThat(cards.sendPlanBatchResult(12, delivery)).isEqualTo(NewsPushPlanBatchResult.RETRY);

        assertThat(sent).hasSize(2);
        assertThat(sent.get(0)).contains(first.getOriginTitle());
        assertThat(sent.get(1)).contains("Next article").doesNotContain(first.getOriginTitle());
        verify(receipts).markPlanDelivered(12, "cli_one", "ou_first", List.of(first.getId()));
        verify(receipts).markPlanDelivered(12, "cli_one", "ou_first", List.of(next.getId()));
        verify(receipts, never()).markPlanDelivered(eq(12L), eq("cli_one"), eq("ou_failed"), anyList());
    }

    @Test
    void previewsThePlanWithItsAppRecipientTemplateAndContentScope() {
        var receipts = mock(NewsFeishuDigestDeliveryMapper.class);
        var cards = new NewsFeishuDigestService(items, receipts, mock(NewsAiFieldCatalog.class));
        var notification = new NewsFeishuNotificationSettingService(settings, new FeishuAppRobotClient(restTemplate),
                webhookClient, properties, cards);
        var channel = new NewsFeishuChannelConfigPO();
        channel.setConfigJson("{\"apps\":[{\"appId\":\"cli_plan\",\"appSecret\":\"plan-secret\"}],"
                + "\"recipients\":[{\"appId\":\"cli_plan\",\"openId\":\"ou_plan\"}],"
                + "\"card\":{\"title\":\"Saved card\",\"template\":{\"id\":\"AAplan\",\"version\":\"2.0\","
                + "\"variablesJson\":\"{\\\"body\\\":\\\"${content}\\\"}\"}},\"content\":{\"channelCodes\":[\"wechat_mp\"]}}");
        when(settings.selectChannel()).thenReturn(channel);
        var plan = new NewsPushPlanDefinition();
        plan.setAppId("cli_plan");
        plan.setRecipientOpenIds(List.of("ou_plan"));
        plan.setCard(NewsPushPlanCard.SAVED);
        var scope = new NewsDigestContentSettings();
        scope.setChannelCodes(List.of("bilibili"));
        plan.setContent(scope);
        when(items.selectRecentDigestItems(NewsItemStatus.APPROVED, scope, 3)).thenReturn(List.of(video()));
        feishu.expect(requestTo(TOKEN_URL)).andExpect(jsonPath("$.client_id").value("cli_plan"))
                .andRespond(withSuccess("{\"code\":0,\"access_token\":\"plan-token\"}", MediaType.APPLICATION_JSON));
        feishu.expect(requestTo(MESSAGE_URL)).andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer plan-token"))
                .andExpect(jsonPath("$.receive_id").value("ou_plan"))
                .andExpect(request -> {
                    var body = JacksonUtil.readTree(((MockClientHttpRequest) request).getBodyAsString());
                    var card = JacksonUtil.readTree(body.path("content").asText());
                    assertThat(card.at("/data/template_id").asText()).isEqualTo("AAplan");
                    assertThat(card.at("/data/template_variable/body").asText()).contains(video().getOriginTitle());
                }).andRespond(withSuccess("{\"code\":0}", MediaType.APPLICATION_JSON));
        var request = new NewsPushPlanPreviewRequest();
        request.setDefinition(plan);
        request.setOpenId("ou_plan");

        notification.verifyPlan(request);

        feishu.verify();
        verifyNoInteractions(receipts);
        request.setOpenId("ou_outside");
        assertThatThrownBy(() -> notification.verifyPlan(request)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("该推送计划");
        plan.setRecipientOpenIds(List.of("ou_plan", "ou_removed"));
        assertThatThrownBy(() -> notification.planDigestDelivery(plan)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("已配置的推送人员");
    }

    @Test
    void sendsTheNativeDigestPreviewWithApprovedItemsAndLeavesDeliveryStateUnchanged() {
        var deliveries = mock(NewsFeishuDigestDeliveryMapper.class);
        var cards = new NewsFeishuDigestService(items, deliveries, mock(NewsAiFieldCatalog.class));
        var notification = new NewsFeishuNotificationSettingService(settings, new FeishuAppRobotClient(restTemplate),
                webhookClient, properties, cards);
        var second = new NewsItemPO();
        second.setOriginTitle("Another approved article");
        second.setCanonicalUrl("https://example.com/article");
        var defaultScope = new NewsDigestContentSettings();
        when(items.selectRecentDigestItems(NewsItemStatus.APPROVED, defaultScope, 3)).thenReturn(List.of(video(), second));
        feishu.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(
                "{\"code\":0,\"access_token\":\"test-app-token\"}", MediaType.APPLICATION_JSON));
        feishu.expect(requestTo(MESSAGE_URL))
                .andExpect(jsonPath("$.receive_id").value("ou_first"))
                .andExpect(jsonPath("$.msg_type").value("interactive"))
                .andExpect(request -> {
                    var message = JacksonUtil.readTree(((MockClientHttpRequest) request).getBodyAsString());
                    var card = JacksonUtil.readTree(message.path("content").asText());
                    assertThat(card.at("/schema").asText()).isEqualTo("2.0");
                    assertThat(card.at("/header/template").asText()).isEqualTo("purple");
                    assertThat(card.at("/header/subtitle/content").asText()).isEqualTo("2 条资讯");
                    assertThat(card.at("/body/elements/0/text/content").asText()).isEqualTo(video().getOriginTitle());
                    assertThat(card.at("/body/elements/1/text/content").asText()).isEqualTo("作者：" + video().getAuthor());
                    assertThat(card.at("/body/elements/2/behaviors/0/default_url").asText()).isEqualTo(VIDEO_URL);
                    assertThat(card.at("/body/elements/3/tag").asText()).isEqualTo("hr");
                    assertThat(card.at("/body/elements/4/text/content").asText()).isEqualTo(second.getOriginTitle());
                })
                .andRespond(withSuccess("{\"code\":0}", MediaType.APPLICATION_JSON));
        var card = new NewsFeishuCardSettings();
        card.setColor(NewsFeishuCardColor.PURPLE);
        var request = new NewsFeishuCardTestRequest();
        request.setAppId("cli_demo");
        request.setAppSecret("test-secret");
        request.setOpenId("ou_first");
        request.setCard(card);

        notification.verifyCard(request);

        feishu.verify();
        verify(items).selectRecentDigestItems(NewsItemStatus.APPROVED, defaultScope, 3);
        verifyNoMoreInteractions(items);
        verifyNoInteractions(deliveries);
    }

    @Test
    void usesTheSameTemplateWithTypedVariablesAndScopedAiFieldsForPreviewAndDelivery() {
        var catalog = mock(NewsAiFieldCatalog.class);
        var deliveries = mock(NewsFeishuDigestDeliveryMapper.class);
        var cards = new NewsFeishuDigestService(items, deliveries, catalog);
        var metric = new NewsAiFieldPO();
        metric.setFieldKey("rating");
        metric.setName("评分");
        when(catalog.listEnabled()).thenReturn(List.of(metric));
        var item = video();
        item.setAiValuesJson("{\"title\":\"AI 标题\",\"rating\":8,\"hidden\":\"不展示\"}");
        var scope = new NewsDigestContentSettings();
        scope.setChannelCodes(List.of("bilibili"));
        scope.setContentTypes(List.of(NewsContentType.VIDEO));
        scope.setReviewDays(7);
        scope.setFields(List.of("ai:rating"));
        scope.setUseAiTitle(true);
        when(items.selectRecentDigestItems(NewsItemStatus.APPROVED, scope, 3)).thenReturn(List.of(item));
        when(items.selectFeishuDigestItemsForUpdate(eq(NewsItemStatus.APPROVED), eq(scope), anyInt()))
                .thenReturn(List.of(item));
        var template = new NewsFeishuCardTemplate();
        template.setId("AAexample");
        template.setVersion("2.1.0");
        template.setVariablesJson("{\"rows\":\"${items}\",\"total\":\"${count}\",\"body\":\"${content}\","
                + "\"fixed\":{\"flag\":true}}");
        var card = new NewsFeishuCardSettings();
        card.setTemplate(template);

        String preview = cards.previewCardContent(card, scope);
        var layout = cards.previewEntries(scope);
        assertThat(layout).singleElement().satisfies(entry -> {
            assertThat(entry.getTitle()).isEqualTo("AI 标题");
            assertThat(entry.getUrl()).isNull();
            assertThat(entry.getFields()).singleElement().satisfies(field -> {
                assertThat(field.getLabel()).isEqualTo("评分");
                assertThat(field.getDisplay()).isEqualTo("8");
            });
        });
        var sent = new ArrayList<String>();
        var target = new NewsFeishuDigestDelivery.Target("cli_one", "ou_one", sent::add);
        assertThat(cards.sendBatch(new NewsFeishuDigestDelivery(card, scope, List.of(target)))).isTrue();
        assertThat(sent).containsExactly(preview);
        var payload = JacksonUtil.readTree(preview);
        assertThat(payload.at("/type").asText()).isEqualTo("template");
        assertThat(payload.at("/data/template_id").asText()).isEqualTo("AAexample");
        assertThat(payload.at("/data/template_version_name").asText()).isEqualTo("2.1.0");
        var variables = payload.at("/data/template_variable");
        assertThat(variables.at("/rows/0/title").asText()).isEqualTo("AI 标题");
        assertThat(variables.at("/rows/0/ai_rating").intValue()).isEqualTo(8);
        assertThat(variables.at("/total").intValue()).isEqualTo(1);
        assertThat(variables.at("/fixed/flag").asBoolean()).isTrue();
        assertThat(variables.at("/body").asText()).isEqualTo("AI 标题\n评分：8");
        assertThat(variables.toString()).doesNotContain(VIDEO_URL, "不展示", item.getAuthor());
        verify(items).markDigestSent(List.of(item.getId()));
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 400})
    void retainsFeishuFailureReasonErrorCodeAndRequestIdForTestSending(int status) {
        feishu.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(
                "{\"code\":0,\"access_token\":\"test-app-token\"}", MediaType.APPLICATION_JSON));
        feishu.expect(requestTo(MESSAGE_URL)).andRespond(withStatus(HttpStatus.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":230099,\"msg\":\"template variable content is required\","
                        + "\"error\":{\"log_id\":\"test-request-123\"}}"));
        var client = new FeishuAppRobotClient(restTemplate);
        var sender = client.cardSender(new FeishuAppRobotClient.AppCredentials("cli_test", "test-secret"));

        assertThatThrownBy(() -> sender.accept("ou_one", "{}"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("230099")
                .hasMessageContaining("template variable content is required")
                .hasMessageContaining("test-request-123");

        feishu.verify();
    }

    @Test
    void retriesTheFailedRecipientWithoutResendingToSuccessfulRecipients() {
        var deliveries = mock(NewsFeishuDigestDeliveryMapper.class);
        var cards = new NewsFeishuDigestService(items, deliveries, mock(NewsAiFieldCatalog.class));
        List<Long> ids = List.of(video().getId());
        when(items.selectFeishuDigestItemsForUpdate(eq(NewsItemStatus.APPROVED), any(), anyInt()))
                .thenReturn(List.of(video()));
        when(deliveries.selectDelivered("cli_one", "ou_one", ids)).thenReturn(List.of(), ids);
        var first = new ArrayList<String>();
        var second = new ArrayList<String>();
        var card = new NewsFeishuCardSettings();
        card.setTitle("精选资讯");
        card.setColor(NewsFeishuCardColor.PURPLE);
        card.setShowAuthor(false);
        var firstTarget = new NewsFeishuDigestDelivery.Target("cli_one", "ou_one", first::add);
        var failed = new NewsFeishuDigestDelivery.Target("cli_two", "ou_two", content -> {
            throw new BusinessException("暂时无法投递");
        });
        assertThat(cards.sendBatch(new NewsFeishuDigestDelivery(card, null, List.of(firstTarget, failed)))).isFalse();
        verify(deliveries).markDelivered("cli_one", "ou_one", ids);
        verify(items, never()).markDigestSent(ids);

        var recovered = new NewsFeishuDigestDelivery.Target("cli_two", "ou_two", second::add);
        assertThat(cards.sendBatch(new NewsFeishuDigestDelivery(card, null, List.of(firstTarget, recovered)))).isTrue();
        assertThat(first).singleElement().asString().contains("精选资讯", "purple", VIDEO_URL)
                .doesNotContain("UP 主");
        assertThat(second).containsExactlyElementsOf(first);
        verify(deliveries).markDelivered("cli_two", "ou_two", ids);
        verify(items).markDigestSent(ids);
        verify(deliveries).deleteByItemIds(ids);
    }

    @Test
    void sendsAnInteractiveCardWithTheAppToken() {
        feishu.expect(requestTo(TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"code\":0,\"access_token\":\"test-app-token\"}", MediaType.APPLICATION_JSON));
        feishu.expect(requestTo(MESSAGE_URL)).andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-app-token"))
                .andExpect(jsonPath("$.receive_id").value("ou_first"))
                .andExpect(jsonPath("$.msg_type").value("interactive"))
                .andExpect(jsonPath("$.content").value("{\"schema\":\"2.0\"}"))
                .andRespond(withSuccess("{\"code\":0}", MediaType.APPLICATION_JSON));
        var client = new FeishuAppRobotClient(restTemplate);
        client.cardSender(new FeishuAppRobotClient.AppCredentials("cli_demo", "test-secret"))
                .accept("ou_first", "{\"schema\":\"2.0\"}");
        feishu.verify();
    }

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
    void reportsFeishuAuthErrorCodeAndCredentialHintWithoutExposingTheResponseBody() {
        when(settings.select()).thenReturn(appSetting(true));
        when(items.selectUnsentDigestItemsForUpdate(eq(NewsItemStatus.APPROVED), anyInt())).thenReturn(List.of(video()));
        feishu.expect(requestTo(TOKEN_URL)).andRespond(withBadRequest().contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":20002,\"error_description\":\"upstream-debug-value\"}"));

        assertThatThrownBy(task::sendDigest).isInstanceOf(BusinessException.class)
                .hasMessageContaining("HTTP 400，code=20002")
                .hasMessageContaining("App Secret")
                .hasMessageNotContaining("upstream-debug-value");

        feishu.verify();
        verify(items, never()).markDigestSent(anyList());
    }

    @Test
    void reportsMissingSendPermissionFromTheMessageApiAfterSuccessfulAuthentication() {
        var appClient = new FeishuAppRobotClient(restTemplate);
        var target = new FeishuAppRobotClient.Recipient("cli_demo", "test-secret", "ou_demo");
        expectToken();
        feishu.expect(requestTo(MESSAGE_URL)).andRespond(withStatus(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON).body("{\"code\":99991672}"));

        assertThatThrownBy(() -> appClient.sender(target).accept("测试消息"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("飞书单聊发送失败，HTTP 403，code=99991672")
                .hasMessageContaining("以应用的身份发消息")
                .hasMessageContaining("获取与发送单聊、群组消息");

        feishu.verify();
    }

    @Test
    void reportsHttpStatusWhenFeishuReturnsANonJsonError() {
        var appClient = new FeishuAppRobotClient(restTemplate);
        var target = new FeishuAppRobotClient.Recipient("cli_demo", "test-secret", "ou_demo");
        feishu.expect(requestTo(TOKEN_URL)).andRespond(withBadRequest().contentType(MediaType.TEXT_PLAIN)
                .body("gateway debug output"));

        assertThatThrownBy(() -> appClient.sender(target).accept("测试消息"))
                .isInstanceOf(BusinessException.class).hasMessage("飞书应用鉴权失败，HTTP 400");

        feishu.verify();
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
                .andExpect(jsonPath("$.scope").doesNotHaveJsonPath())
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
