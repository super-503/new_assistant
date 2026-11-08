package com.zengame.ai.news;

import com.zengame.ai.apiclient.feishu.FeishuRobotClient;
import com.zengame.ai.business.news.NewsAiFieldCatalog;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldKind;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldSource;
import com.zengame.ai.business.news.processing.JobFamilySyncService;
import com.zengame.ai.business.news.review.domain.bo.NewsItemViewContext;
import com.zengame.ai.business.news.setting.NewsAiFieldSettingService;
import com.zengame.ai.business.news.setting.NewsAiModelSettingService;
import com.zengame.ai.business.news.setting.domain.vo.NewsAiFieldVO;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.CenterUserMapper;
import com.zengame.ai.repository.ai.mapper.NewsAiFieldMapper;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.zengame.ai.news.NewsAiFieldFixtures.field;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NewsJobFamilyAvailabilityTest {

    private final NewsProperties properties = new NewsProperties();
    private final NewsAiFieldMapper mapper = mock(NewsAiFieldMapper.class);
    private final NewsAiFieldCatalog catalog = new NewsAiFieldCatalog(properties, mapper);

    @Test
    void excludesPopulatedLegacyJobFamilyButKeepsItsNameForHistory() {
        NewsAiFieldPO topic = topic();
        NewsAiFieldPO jobFamily = jobFamily();
        List<NewsAiFieldPO> configured = List.of(topic, jobFamily);
        when(mapper.selectEnabled()).thenReturn(configured);

        NewsItemViewContext context = new NewsItemViewContext(configured, catalog::isAvailable, true,
                List.of(), List.of(), List.of());

        assertThat(catalog.listEnabled()).containsExactly(topic);
        assertThat(context.tagFields()).containsExactly(topic);
        assertThat(context.fieldName("job_family")).isEqualTo("岗位分类");

        properties.setJobFamilyEnabled(true);
        assertThat(catalog.listEnabled()).containsExactly(topic, jobFamily);
    }

    @Test
    void sortsVisibleSettingsWithTheHiddenFieldInItsOriginalPosition() {
        NewsAiFieldPO topic = topic();
        NewsAiFieldPO jobFamily = jobFamily();
        NewsAiFieldPO summary = field("summary", NewsAiFieldSource.BODY, NewsAiFieldOutputType.TEXT, List.of());
        summary.setId(3L);
        summary.setSortNo(3);
        when(mapper.selectAll()).thenReturn(List.of(topic, jobFamily, summary));
        NewsAiModelSettingService model = mock(NewsAiModelSettingService.class);
        when(model.isConfigured()).thenReturn(true);
        NewsAiFieldSettingService settings = new NewsAiFieldSettingService(mapper, catalog, model);

        assertThat(settings.list()).extracting(NewsAiFieldVO::getFieldKey).containsExactly("topic", "summary");
        assertThatThrownBy(() -> settings.sort(List.of(3L, 2L, 1L))).isInstanceOf(BusinessException.class);

        settings.sort(List.of(3L, 1L));

        verify(mapper).updateSortNo(3L, 1);
        verify(mapper).updateSortNo(1L, 3);
        verify(mapper, never()).updateSortNo(eq(2L), anyInt());
    }

    @Test
    void skipsEmployeeLookupEvenIfSyncWasPreviouslyEnabled() {
        properties.setSyncJobFamilies(true);
        CenterUserMapper users = mock(CenterUserMapper.class);
        FeishuRobotClient robot = mock(FeishuRobotClient.class);

        NewsAiModelSettingService model = mock(NewsAiModelSettingService.class);
        new JobFamilySyncService(properties, model, users, mapper, robot).sync();

        verifyNoInteractions(model, users, mapper, robot);
    }

    @Test
    void skipsEmployeeLookupWithoutAModel() {
        properties.setJobFamilyEnabled(true);
        properties.setSyncJobFamilies(true);
        CenterUserMapper users = mock(CenterUserMapper.class);
        FeishuRobotClient robot = mock(FeishuRobotClient.class);
        NewsAiModelSettingService model = mock(NewsAiModelSettingService.class);

        new JobFamilySyncService(properties, model, users, mapper, robot).sync();

        verifyNoInteractions(users, mapper, robot);
    }

    private NewsAiFieldPO topic() {
        NewsAiFieldPO field = field("topic", NewsAiFieldSource.TITLE_BODY, NewsAiFieldOutputType.MULTI, List.of("技术"));
        field.setId(1L);
        field.setShowAsTag(true);
        return field;
    }

    private NewsAiFieldPO jobFamily() {
        NewsAiFieldPO field = field("job_family", NewsAiFieldSource.TITLE_BODY, NewsAiFieldOutputType.MULTI, List.of("技术族"));
        field.setId(2L);
        field.setName("岗位分类");
        field.setKind(NewsAiFieldKind.BUILTIN);
        field.setSortNo(2);
        return field;
    }
}
