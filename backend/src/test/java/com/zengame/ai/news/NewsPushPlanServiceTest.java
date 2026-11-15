package com.zengame.ai.news;

import com.zengame.ai.business.news.NewsPushPlanController;
import com.zengame.ai.apiclient.feishu.FeishuDigestCard;
import com.zengame.ai.business.news.library.NewsFeishuDigestService;
import com.zengame.ai.business.news.setting.NewsFeishuNotificationSettingService;
import com.zengame.ai.business.news.setting.NewsPushPlanService;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuDigestDelivery;
import com.zengame.ai.business.news.setting.domain.bo.NewsPushPlanDefinition;
import com.zengame.ai.business.news.setting.domain.enums.NewsPushPlanBatchResult;
import com.zengame.ai.business.news.setting.domain.request.NewsPushPlanPreviewRequest;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.WebExceptionAdapter;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.mapper.NewsPushPlanMapper;
import com.zengame.ai.repository.ai.po.NewsPushPlanPO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NewsPushPlanServiceTest {
    private static final String PATH = "/ai/api/web/v1/news/setting/push-plans";
    private static final String BODY = "{\"enabled\":true,\"definition\":{\"name\":\"Daily\",\"sendTime\":\"08:30\","
            + "\"channel\":\"FEISHU\",\"appId\":\"cli_one\",\"recipientOpenIds\":[\"ou_one\"],\"card\":\"SAVED\","
            + "\"content\":{\"channelCodes\":[\"bilibili\"],\"contentTypes\":[\"VIDEO\"],\"reviewDays\":7}}}";
    private final NewsPushPlanMapper mapper = mock(NewsPushPlanMapper.class);
    private final NewsFeishuNotificationSettingService feishu = mock(NewsFeishuNotificationSettingService.class);
    private final NewsFeishuDigestService digest = mock(NewsFeishuDigestService.class);
    private final NewsPushPlanService service = new NewsPushPlanService(mapper, new NewsProperties(), feishu, digest);

    @Test
    void persistsTheSelectedOptionsAndSchedulesTheNextDayTimeInTheProjectTimeZone() throws Exception {
        when(mapper.insert(any())).thenAnswer(invocation -> {
            invocation.<NewsPushPlanPO>getArgument(0).setId(3L);
            return 1;
        });
        var mvc = MockMvcBuilders.standaloneSetup(new NewsPushPlanController(service))
                .setControllerAdvice(new WebExceptionAdapter()).build();
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(3));

        var record = ArgumentCaptor.forClass(NewsPushPlanPO.class);
        verify(mapper).insert(record.capture());
        assertThat(record.getValue().getNextRunAt()).isAfter(LocalDateTime.now(ZoneOffset.UTC));
        assertThat(record.getValue().getNextRunAt().toLocalTime()).isEqualTo(LocalTime.of(0, 30));
        when(mapper.selectAll()).thenReturn(List.of(record.getValue()));
        mvc.perform(get(PATH)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timeZone").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.data.plans[0].definition.recipientOpenIds[0]").value("ou_one"))
                .andExpect(jsonPath("$.data.plans[0].definition.card").value("SAVED"))
                .andExpect(jsonPath("$.data.plans[0].definition.content.reviewDays").value(7));
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY.replace("08:30", "25:30")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void advancesTheScheduleOnlyWhenTheDueRunFinishes() {
        var definition = JacksonUtil.getObjectMapper().convertValue(JacksonUtil.readTree(BODY).path("definition"),
                NewsPushPlanDefinition.class);
        var record = new NewsPushPlanPO();
        record.setId(8L);
        record.setEnabled(true);
        record.setConfigJson(JacksonUtil.writeValueAsString(definition));
        record.setNextRunAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(2));
        when(mapper.selectForUpdate(8)).thenReturn(record);
        var delivery = new NewsFeishuDigestDelivery(null, definition.getContent(), List.of(
                new NewsFeishuDigestDelivery.Target("cli_one", "ou_one", content -> { })));
        when(feishu.planDigestDelivery(definition)).thenReturn(delivery);
        when(digest.sendPlanBatchResult(8, delivery))
                .thenReturn(NewsPushPlanBatchResult.MORE, NewsPushPlanBatchResult.COMPLETE);

        assertThat(service.runDueBatch(8)).isTrue();
        verify(mapper, never()).updateNextRunAt(eq(8L), any());
        assertThat(service.runDueBatch(8)).isFalse();
        var nextRun = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).updateNextRunAt(eq(8L), nextRun.capture());
        assertThat(nextRun.getValue()).isAfter(LocalDateTime.now(ZoneOffset.UTC));
        assertThat(nextRun.getValue().toLocalTime()).isEqualTo(LocalTime.of(0, 30));
    }

    @Test
    void schedulesPartialFailureForRetryBeforeTheNextDailyRun() {
        var definition = JacksonUtil.getObjectMapper().convertValue(JacksonUtil.readTree(BODY).path("definition"),
                NewsPushPlanDefinition.class);
        var record = new NewsPushPlanPO();
        record.setEnabled(true);
        record.setConfigJson(JacksonUtil.writeValueAsString(definition));
        record.setNextRunAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(2));
        when(mapper.selectForUpdate(8)).thenReturn(record);
        var delivery = new NewsFeishuDigestDelivery(null, definition.getContent(), List.of(
                new NewsFeishuDigestDelivery.Target("cli_one", "ou_one", content -> { })));
        when(feishu.planDigestDelivery(definition)).thenReturn(delivery);
        when(digest.sendPlanBatchResult(8, delivery)).thenReturn(NewsPushPlanBatchResult.RETRY);

        assertThat(service.runDueBatch(8)).isFalse();

        var retry = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).updateNextRunAt(eq(8L), retry.capture());
        assertThat(retry.getValue()).isBetween(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(4),
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(6));
    }

    @Test
    void keepsAnInterruptedChannelDueForRetryWhenItIsEnabledAgain() {
        var definition = JacksonUtil.getObjectMapper().convertValue(JacksonUtil.readTree(BODY).path("definition"),
                NewsPushPlanDefinition.class);
        var record = new NewsPushPlanPO();
        record.setEnabled(true);
        record.setConfigJson(JacksonUtil.writeValueAsString(definition));
        record.setNextRunAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(2));
        when(mapper.selectForUpdate(8)).thenReturn(record);
        when(feishu.planDigestDelivery(definition)).thenReturn(
                new NewsFeishuDigestDelivery(null, definition.getContent(), List.of()));

        assertThat(service.runDueBatch(8)).isFalse();

        var retry = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).updateNextRunAt(eq(8L), retry.capture());
        assertThat(retry.getValue()).isBetween(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(4),
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(6));
        verify(digest, never()).sendPlanBatchResult(anyLong(), any());
    }

    @Test
    void previewsTheSubmittedPlanWithoutSavingOrRunningThePlan() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new NewsPushPlanController(service))
                .setControllerAdvice(new WebExceptionAdapter()).build();
        String previewBody = "{\"definition\":" + JacksonUtil.readTree(BODY).path("definition") + ",\"openId\":\"ou_one\"}";

        mvc.perform(post(PATH + "/preview").contentType(MediaType.APPLICATION_JSON).content(previewBody))
                .andExpect(status().isOk());

        var request = ArgumentCaptor.forClass(NewsPushPlanPreviewRequest.class);
        verify(feishu).verifyPlan(request.capture());
        assertThat(request.getValue().getDefinition().getAppId()).isEqualTo("cli_one");
        assertThat(request.getValue().getDefinition().getContent().getChannelCodes()).containsExactly("bilibili");
        assertThat(request.getValue().getOpenId()).isEqualTo("ou_one");
        verifyNoInteractions(mapper, digest);
    }

    @Test
    void previewsOnlyTheContentSettingsWithoutRequiringAnAppOrSavingAPlan() throws Exception {
        when(digest.previewEntries(any())).thenReturn(List.of(new FeishuDigestCard.Entry(
                "Preview title", "https://example.com/article", List.of())));
        var mvc = MockMvcBuilders.standaloneSetup(new NewsPushPlanController(service))
                .setControllerAdvice(new WebExceptionAdapter()).build();

        mvc.perform(post(PATH + "/content-preview").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channelCodes\":[\"bilibili\"],\"fields\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Preview title"))
                .andExpect(jsonPath("$.data[0].url").value("https://example.com/article"))
                .andExpect(jsonPath("$.data[0].fields").isEmpty());

        verifyNoInteractions(mapper, feishu);
    }
}
