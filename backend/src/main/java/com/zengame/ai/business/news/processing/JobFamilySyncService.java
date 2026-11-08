package com.zengame.ai.business.news.processing;

import com.zengame.ai.apiclient.feishu.FeishuRobotClient;
import com.zengame.ai.business.news.setting.NewsAiModelSettingService;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.repository.ai.mapper.CenterUserMapper;
import com.zengame.ai.repository.ai.mapper.NewsAiFieldMapper;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.zengame.ai.business.news.NewsAiFieldCatalog.JOB_FAMILY_FIELD_KEY;

/**
 * 岗位分类同步：岗位分类字段的选项就是在职员工当前的职位族集合。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobFamilySyncService {

    private static final String NONE = "无";

    private final NewsProperties newsProperties;

    private final NewsAiModelSettingService newsAiModelSettingService;

    private final CenterUserMapper centerUserMapper;

    private final NewsAiFieldMapper newsAiFieldMapper;

    private final FeishuRobotClient feishuRobotClient;

    /**
     * 已配置模型且岗位分类和职位族同步都启用时，有变化便更新岗位选项并发飞书通知。
     * 首次从空选项同步到有值时同样通知。已打在资讯上的旧标签保留。
     *
     * @throws IllegalStateException 岗位分类字段不存在，或在职员工的职位族为空
     */
    public void sync() {
        if (!newsProperties.isJobFamilyEnabled() || !newsProperties.isSyncJobFamilies()
                || !newsAiModelSettingService.isConfigured()) {
            return;
        }
        List<String> jobFamilies = centerUserMapper.selectActiveJobFamilies();
        if (jobFamilies.isEmpty()) {
            throw new IllegalStateException("在职员工的职位族为空，岗位分类选项不同步");
        }
        NewsAiFieldPO field = Optional.ofNullable(newsAiFieldMapper.selectByFieldKey(JOB_FAMILY_FIELD_KEY))
                .orElseThrow(() -> new IllegalStateException("岗位分类字段不存在: " + JOB_FAMILY_FIELD_KEY));
        Set<String> current = new LinkedHashSet<>(StringListJson.read(field.getOptionsJson()));
        List<String> added = jobFamilies.stream().filter(name -> !current.contains(name)).collect(Collectors.toList());
        List<String> removed = current.stream().filter(name -> !jobFamilies.contains(name)).collect(Collectors.toList());
        if (added.isEmpty() && removed.isEmpty()) {
            return;
        }
        field.setOptionsJson(StringListJson.write(jobFamilies));
        newsAiFieldMapper.updateById(field);
        log.info("岗位分类选项已同步, added={}, removed={}", added, removed);
        feishuRobotClient.sendRobotMessage("岗位族变更：新增 " + joinOrNone(added) + "，移除 " + joinOrNone(removed)
                + "，请检查岗位分类提示词");
    }

    private String joinOrNone(List<String> names) {
        return Optional.of(String.join("、", names))
                .filter(text -> !text.isEmpty())
                .orElse(NONE);
    }
}
