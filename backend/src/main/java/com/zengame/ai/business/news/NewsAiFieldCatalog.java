package com.zengame.ai.business.news;

import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.repository.ai.mapper.NewsAiFieldMapper;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/** AI 字段的运行时可用范围，独立于数据库中保存的字段配置。 */
@Component
@RequiredArgsConstructor
public class NewsAiFieldCatalog {

    public static final String JOB_FAMILY_FIELD_KEY = "job_family";

    private final NewsProperties newsProperties;

    private final NewsAiFieldMapper newsAiFieldMapper;

    public boolean isAvailable(NewsAiFieldPO field) {
        return newsProperties.isJobFamilyEnabled() || !JOB_FAMILY_FIELD_KEY.equalsIgnoreCase(field.getFieldKey());
    }

    public List<NewsAiFieldPO> listEnabled() {
        return newsAiFieldMapper.selectEnabled().stream()
                .filter(this::isAvailable)
                .collect(Collectors.toList());
    }
}
