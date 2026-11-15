package com.zengame.ai.news;

import com.zengame.ai.apiclient.feishu.FeishuAppRobotClient;
import com.zengame.ai.apiclient.feishu.FeishuRobotClient;
import com.zengame.ai.business.news.NewsSettingController;
import com.zengame.ai.business.news.library.NewsFeishuDigestService;
import com.zengame.ai.business.news.setting.NewsFeishuAuthorizationService;
import com.zengame.ai.business.news.setting.NewsFeishuNotificationSettingService;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.WebExceptionAdapter;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.mapper.NewsFeishuNotificationSettingMapper;
import com.zengame.ai.repository.ai.po.NewsFeishuAuthorizedUserPO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NewsFeishuAuthorizationTest {

    private static final String PATH = "/ai/api/web/v1/news/setting/notification-channel/authorization";
    private static final String CALLBACK = "http://127.0.0.1:8091" + PATH + "/callback";
    private static final String TOKEN_URL = "https://open.feishu.cn/open-apis/authen/v2/oauth/token";
    private static final String USER_INFO_URL = "https://open.feishu.cn/open-apis/authen/v1/user_info";

    private final NewsFeishuNotificationSettingMapper mapper = mock(NewsFeishuNotificationSettingMapper.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer feishu = MockRestServiceServer.bindTo(restTemplate).build();
    private final MockHttpSession session = new MockHttpSession();
    private final MockMvc mvc = buildMvc();

    @Test
    void authorizesTheDraftAppAndReturnsItsUserOnlyToTheSameAdminSession() throws Exception {
        String state = start();
        mvc.perform(get(PATH + "/result").session(new MockHttpSession()).param("state", state))
                .andExpect(status().isBadRequest());

        feishu.expect(requestTo(TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.grant_type").value("authorization_code"))
                .andExpect(jsonPath("$.client_id").value("cli_demo"))
                .andExpect(jsonPath("$.client_secret").value("draft-secret"))
                .andExpect(jsonPath("$.code").value("test-code"))
                .andExpect(jsonPath("$.redirect_uri").value(CALLBACK))
                .andExpect(jsonPath("$.code_verifier").value(matchesPattern("[A-Za-z0-9_-]{43}")))
                .andRespond(withSuccess("{\"code\":0,\"access_token\":\"test-user-token\"}", MediaType.APPLICATION_JSON));
        feishu.expect(requestTo(USER_INFO_URL)).andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-user-token"))
                .andRespond(withSuccess("{\"code\":0,\"data\":{\"open_id\":\"ou_demo\",\"name\":\"飞书用户\","
                        + "\"avatar_url\":\"https://example.com/avatar.png\"}}", MediaType.APPLICATION_JSON));

        mvc.perform(get(PATH + "/callback").session(new MockHttpSession()).param("state", state).param("code", "test-code"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(PATH + "/callback").session(session).param("state", state).param("code", "test-code"))
                .andExpect(status().isOk());
        mvc.perform(get(PATH + "/callback").session(session).param("state", state).param("code", "test-code"))
                .andExpect(status().isBadRequest());
        String response = mvc.perform(get(PATH + "/result").session(session).param("state", state))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JacksonUtil.readTree(response).path("data").path("openId").asText()).isEqualTo("ou_demo");
        assertThat(response).doesNotContain("test-user-token", "draft-secret");
        feishu.verify();
        var record = ArgumentCaptor.forClass(NewsFeishuAuthorizedUserPO.class);
        verify(mapper).upsertAuthorizedUser(record.capture());
        assertThat(record.getValue().getAppId()).isEqualTo("cli_demo");
        assertThat(record.getValue().getOpenId()).isEqualTo("ou_demo");
        assertThat(record.getValue().getName()).isEqualTo("飞书用户");
        when(mapper.selectAuthorizedUsers("cli_demo")).thenReturn(List.of(record.getValue()));
        String directory = PATH.replace("/authorization", "/feishu/authorized-users");
        mvc.perform(get(directory).param("appId", "cli_demo"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].openId")
                        .value("ou_demo"));
        mvc.perform(get(directory).param("appId", "cli_other"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data").isEmpty());
    }

    @Test
    void returnsTheBotNameAndAvatarForTheDraftApp() throws Exception {
        feishu.expect(requestTo("https://accounts.feishu.cn/oauth/v3/token")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.client_id").value("cli_demo"))
                .andRespond(withSuccess("{\"code\":0,\"access_token\":\"bot-token\"}", MediaType.APPLICATION_JSON));
        feishu.expect(requestTo("https://open.feishu.cn/open-apis/bot/v3/info")).andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer bot-token"))
                .andRespond(withSuccess("{\"code\":0,\"bot\":{\"app_name\":\"资讯日报\","
                        + "\"avatar_url\":\"https://example.com/bot.png\"}}", MediaType.APPLICATION_JSON));

        String response = mvc.perform(post(PATH.replace("/authorization", "/bot-profile"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"cli_demo\",\"appSecret\":\"draft-secret\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(
                        java.nio.charset.StandardCharsets.UTF_8);

        var profile = JacksonUtil.readTree(response).path("data");
        assertThat(profile.path("name").asText()).isEqualTo("资讯日报");
        assertThat(profile.path("avatarUrl").asText()).isEqualTo("https://example.com/bot.png");
        assertThat(response).doesNotContain("bot-token", "draft-secret");
        feishu.verify();
    }

    @Test
    void returnsCancellationToTheInitiatingSessionWithoutExchangingAToken() throws Exception {
        String state = start();
        mvc.perform(get(PATH + "/callback").session(session).param("state", state).param("error", "access_denied"))
                .andExpect(status().isOk());
        String response = mvc.perform(get(PATH + "/result").session(session).param("state", state))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(JacksonUtil.readTree(response).path("msg").asText()).contains("飞书授权已取消");
        feishu.verify();
    }

    private String start() throws Exception {
        var response = mvc.perform(post(PATH).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"cli_demo\",\"appSecret\":\"draft-secret\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var data = JacksonUtil.readTree(response).path("data");
        String state = data.path("state").asText();
        assertThat(state).matches("[A-Za-z0-9_-]{43}");
        assertThat(data.path("authorizationUrl").asText())
                .startsWith("https://accounts.feishu.cn/open-apis/authen/v1/authorize?")
                .contains("client_id=cli_demo", "response_type=code", "code_challenge_method=S256", "state=" + state)
                .doesNotContain("draft-secret");
        return state;
    }

    private MockMvc buildMvc() {
        NewsProperties properties = new NewsProperties();
        properties.setFeishuAuthorizationRedirectUri(CALLBACK);
        FeishuAppRobotClient client = new FeishuAppRobotClient(restTemplate);
        NewsFeishuNotificationSettingService settings = new NewsFeishuNotificationSettingService(mapper, client,
                mock(FeishuRobotClient.class), properties, mock(NewsFeishuDigestService.class));
        NewsFeishuAuthorizationService authorization = new NewsFeishuAuthorizationService(settings, client, properties);
        return MockMvcBuilders.standaloneSetup(new NewsSettingController(null, null, settings, authorization, null))
                .setControllerAdvice(new WebExceptionAdapter()).build();
    }
}
