package com.zengame.ai.business.news.setting;

import com.zengame.ai.business.news.NewsAiFieldCatalog;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldKind;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import com.zengame.ai.business.news.processing.StringListJson;
import com.zengame.ai.business.news.setting.cover.NewsAiFieldConvert;
import com.zengame.ai.business.news.setting.domain.request.NewsAiFieldSaveRequest;
import com.zengame.ai.business.news.setting.domain.vo.NewsAiFieldVO;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.NewsAiFieldMapper;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 字段配置：字段的增删改、启停和排序。配置变更只影响后续加工，已有资讯保留当前结果。
 *
 * <p>按种类限制可修改的项：系统字段不能删除，字段标识和输出类型不能改；内置字段只能改提示词，不能删除；普通字段全部可改，可以删除。
 */
@Service
@RequiredArgsConstructor
public class NewsAiFieldSettingService {

    private static final int OPTIONS_JSON_MAX_LENGTH = 4000;

    private final NewsAiFieldMapper newsAiFieldMapper;

    private final NewsAiFieldCatalog newsAiFieldCatalog;

    private final NewsAiModelSettingService newsAiModelSettingService;

    /**
     * @return 当前可配置的 AI 字段，按展示顺序
     */
    public List<NewsAiFieldVO> list() {
        if (!newsAiModelSettingService.isConfigured()) {
            return List.of();
        }
        List<NewsAiFieldPO> fields = newsAiFieldMapper.selectAll().stream()
                .filter(newsAiFieldCatalog::isAvailable)
                .collect(Collectors.toList());
        return NewsAiFieldConvert.INSTANCE.toVOList(fields);
    }

    /**
     * 新增普通字段，排在最后。
     *
     * @param request 字段配置
     * @return 新字段的主键
     * @throws BusinessException 配置不合法或字段标识已存在
     */
    public Long create(NewsAiFieldSaveRequest request) {
        newsAiModelSettingService.requireConfigured();
        NewsAiFieldPO field = NewsAiFieldConvert.INSTANCE.toPO(request);
        field.setKind(NewsAiFieldKind.CUSTOM);
        field.setSortNo(nextSortNo());
        validate(field, null);
        newsAiFieldMapper.insert(field);
        return field.getId();
    }

    /**
     * 编辑字段配置。
     *
     * @param id      字段主键
     * @param request 字段配置，锁定项须与当前值一致
     * @throws BusinessException 字段不存在、修改了锁定项、配置不合法或字段标识已存在
     */
    public void update(Long id, NewsAiFieldSaveRequest request) {
        newsAiModelSettingService.requireConfigured();
        NewsAiFieldPO field = load(id);
        checkLocked(field, NewsAiFieldConvert.INSTANCE.toPO(request));
        NewsAiFieldConvert.INSTANCE.updatePO(request, field);
        validate(field, id);
        newsAiFieldMapper.updateById(field);
    }

    /**
     * 删除普通字段。
     *
     * @param id 字段主键
     * @throws BusinessException 字段不存在，或是系统字段、内置字段
     */
    public void delete(Long id) {
        newsAiModelSettingService.requireConfigured();
        NewsAiFieldPO field = load(id);
        if (field.getKind() != NewsAiFieldKind.CUSTOM) {
            throw new BusinessException(field.getKind().getDesc() + "字段「" + field.getName() + "」不能删除");
        }
        newsAiFieldMapper.deleteById(id);
    }

    /**
     * 启用或停用字段。停用后不再生成和展示，已有数据保留。
     *
     * @param id      字段主键
     * @param enabled 是否启用
     * @throws BusinessException 字段不存在或是内置字段
     */
    public void setEnabled(Long id, boolean enabled) {
        newsAiModelSettingService.requireConfigured();
        NewsAiFieldPO field = load(id);
        if (field.getKind() == NewsAiFieldKind.BUILTIN) {
            throw new BusinessException("内置字段「" + field.getName() + "」只能修改提示词");
        }
        field.setEnabled(enabled);
        newsAiFieldMapper.updateById(field);
    }

    /**
     * 按传入顺序重排可配置的字段，暂不可用的字段保留在原有位置。
     *
     * @param ids 当前可配置的字段主键，按新的展示顺序
     * @throws BusinessException 传入的主键与可配置的字段不一致
     */
    @Transactional(rollbackFor = Exception.class)
    public void sort(List<Long> ids) {
        newsAiModelSettingService.requireConfigured();
        List<NewsAiFieldPO> allFields = newsAiFieldMapper.selectAll();
        Map<Long, NewsAiFieldPO> fields = allFields.stream()
                .filter(newsAiFieldCatalog::isAvailable)
                .collect(Collectors.toMap(NewsAiFieldPO::getId, Function.identity()));
        if (ids.size() != fields.size() || !new HashSet<>(ids).equals(fields.keySet())) {
            throw new BusinessException("排序需要传入全部可配置字段，且每个字段只出现一次");
        }
        int visibleIndex = 0;
        for (int i = 0; i < allFields.size(); i++) {
            NewsAiFieldPO field = allFields.get(i);
            if (newsAiFieldCatalog.isAvailable(field)) {
                field = fields.get(ids.get(visibleIndex++));
            }
            int sortNo = i + 1;
            if (field.getSortNo() != sortNo) {
                newsAiFieldMapper.updateSortNo(field.getId(), sortNo);
            }
        }
    }

    /**
     * 校验编辑没有改动种类锁定的项：系统字段锁定字段标识和输出类型；内置字段锁定除提示词以外的所有项。
     *
     * @param current 当前配置
     * @param draft   请求里的配置
     */
    private void checkLocked(NewsAiFieldPO current, NewsAiFieldPO draft) {
        if (current.getKind() == NewsAiFieldKind.SYSTEM
                && (!current.getFieldKey().equals(draft.getFieldKey()) || current.getOutputType() != draft.getOutputType())) {
            throw new BusinessException("系统字段「" + current.getName() + "」的字段标识和输出类型不能修改");
        }
        if (current.getKind() == NewsAiFieldKind.BUILTIN && !sameExceptPrompt(current, draft)) {
            throw new BusinessException("内置字段「" + current.getName() + "」只能修改提示词");
        }
    }

    private boolean sameExceptPrompt(NewsAiFieldPO current, NewsAiFieldPO draft) {
        return Objects.equals(current.getName(), draft.getName())
                && Objects.equals(current.getFieldKey(), draft.getFieldKey())
                && current.getSource() == draft.getSource()
                && current.getOutputType() == draft.getOutputType()
                && Objects.equals(current.getMultiline(), draft.getMultiline())
                && StringListJson.read(current.getOptionsJson()).equals(StringListJson.read(draft.getOptionsJson()))
                && Objects.equals(current.getShowAsTag(), draft.getShowAsTag())
                && Objects.equals(current.getRequired(), draft.getRequired())
                && Objects.equals(current.getEditable(), draft.getEditable())
                && Objects.equals(current.getEnabled(), draft.getEnabled());
    }

    /**
     * 校验字段配置：选项和显示为标签只属于单选、多选；多行只属于文本；选项不为空白、不重复；字段标识唯一。
     *
     * @param field  待保存的配置
     * @param selfId 编辑时为字段自身主键，新增时为空
     */
    private void validate(NewsAiFieldPO field, Long selfId) {
        if (!newsAiFieldCatalog.isAvailable(field)) {
            throw new BusinessException("字段标识当前不可用: " + field.getFieldKey());
        }
        boolean choice = field.getOutputType() == NewsAiFieldOutputType.SINGLE
                || field.getOutputType() == NewsAiFieldOutputType.MULTI;
        List<String> options = StringListJson.read(field.getOptionsJson());
        if (!choice && !options.isEmpty()) {
            throw new BusinessException("只有单选、多选字段可以设置选项");
        }
        if (!choice && BooleanUtils.isTrue(field.getShowAsTag())) {
            throw new BusinessException("只有单选、多选字段可以显示为标签");
        }
        if (field.getOutputType() != NewsAiFieldOutputType.TEXT && BooleanUtils.isTrue(field.getMultiline())) {
            throw new BusinessException("只有文本字段可以设为多行");
        }
        if (options.stream().anyMatch(StringUtils::isBlank)) {
            throw new BusinessException("选项不能为空白");
        }
        Set<String> distinct = new HashSet<>(options);
        if (distinct.size() != options.size()) {
            throw new BusinessException("选项不能重复");
        }
        if (field.getOptionsJson().length() > OPTIONS_JSON_MAX_LENGTH) {
            throw new BusinessException("选项总长度超出限制");
        }
        Optional.ofNullable(newsAiFieldMapper.selectByFieldKey(field.getFieldKey()))
                .filter(other -> !other.getId().equals(selfId))
                .ifPresent(other -> {
                    throw new BusinessException("字段标识「" + field.getFieldKey() + "」已被字段「" + other.getName() + "」使用");
                });
    }

    private NewsAiFieldPO load(Long id) {
        return Optional.ofNullable(newsAiFieldMapper.selectById(id))
                .filter(newsAiFieldCatalog::isAvailable)
                .orElseThrow(() -> new BusinessException("AI 字段不存在: " + id));
    }

    private int nextSortNo() {
        return newsAiFieldMapper.selectAll().stream()
                .mapToInt(NewsAiFieldPO::getSortNo)
                .max()
                .orElse(0) + 1;
    }
}
