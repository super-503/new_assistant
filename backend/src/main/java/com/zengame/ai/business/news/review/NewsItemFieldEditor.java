package com.zengame.ai.business.news.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldOutputType;
import com.zengame.ai.business.news.domain.enums.NewsItemAction;
import com.zengame.ai.business.news.review.domain.bo.NewsFieldEditBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemLogDetailBO;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 人工修改资讯 AI 字段：校验新值、写入结果、记录已修改字段和 FIELD_EDITED 处理记录，以及必填校验。
 */
@Component
@RequiredArgsConstructor
public class NewsItemFieldEditor {

    private final NewsItemLogRecorder newsItemLogRecorder;

    /**
     * 把修改写入资讯：只有值真正变化的字段计入修改，这些字段移出未生成列表、记入已修改列表。
     *
     * @param item          资讯，就地修改
     * @param allowedFields 本次允许修改的字段
     * @param edits         修改内容
     * @return 值有变化的字段，按修改内容的顺序
     * @throws BusinessException 字段不允许修改或重复提交、值不符合字段的输出类型或选项、没有任何字段变化
     */
    public List<NewsAiFieldPO> apply(NewsItemBO item, List<NewsAiFieldPO> allowedFields, List<NewsFieldEditBO> edits) {
        Map<String, NewsAiFieldPO> allowed = allowedFields.stream()
                .collect(Collectors.toMap(NewsAiFieldPO::getFieldKey, Function.identity()));
        Set<String> submitted = new HashSet<>();
        List<NewsAiFieldPO> changed = new ArrayList<>();
        for (NewsFieldEditBO edit : edits) {
            NewsAiFieldPO field = Optional.ofNullable(allowed.get(edit.getFieldKey()))
                    .orElseThrow(() -> new BusinessException("字段不允许修改: " + edit.getFieldKey()));
            if (!submitted.add(field.getFieldKey())) {
                throw new BusinessException("字段重复提交: " + field.getName());
            }
            Optional<JsonNode> current = item.value(field.getFieldKey());
            JsonNode value = normalize(field, edit.getValue(), current);
            if (!current.map(old -> sameValue(field, old, value)).orElse(false)) {
                item.getAiValues().set(field.getFieldKey(), value);
                changed.add(field);
            }
        }
        if (changed.isEmpty()) {
            throw new BusinessException("字段没有修改");
        }
        List<String> changedKeys = changed.stream().map(NewsAiFieldPO::getFieldKey).collect(Collectors.toList());
        List<String> failedKeys = new ArrayList<>(item.getAiFailedKeys());
        failedKeys.removeAll(changedKeys);
        item.setAiFailedKeys(failedKeys);
        Set<String> editedKeys = new LinkedHashSet<>(item.getEditedKeys());
        editedKeys.addAll(changedKeys);
        item.setEditedKeys(new ArrayList<>(editedKeys));
        return changed;
    }

    /**
     * 校验必填字段都有值：能取值的必填字段不能未生成或为空。
     *
     * @param item   资讯
     * @param fields 参与校验的字段，按展示顺序
     * @throws BusinessException 第一个不满足的字段，提示「请填写 X」或「请选择 X」
     */
    public void checkRequired(NewsItemBO item, List<NewsAiFieldPO> fields) {
        fields.stream()
                .filter(NewsAiFieldPO::getRequired)
                .filter(NewsAiFieldRules::isFillable)
                .filter(field -> NewsAiFieldRules.isMissing(field, item)
                        || item.value(field.getFieldKey()).map(NewsAiFieldRules::isEmptyValue).orElse(true))
                .findFirst()
                .ifPresent(field -> {
                    throw new BusinessException(NewsAiFieldRules.requiredMessage(field));
                });
    }

    /**
     * 追加 FIELD_EDITED 处理记录，附带修改的字段。
     *
     * @param itemId   资讯
     * @param changed  值有变化的字段
     * @param operator 操作人登录名
     */
    public void recordEdited(Long itemId, List<NewsAiFieldPO> changed, String operator) {
        NewsItemLogDetailBO detail = new NewsItemLogDetailBO();
        detail.setFieldKeys(changed.stream().map(NewsAiFieldPO::getFieldKey).collect(Collectors.toList()));
        newsItemLogRecorder.record(itemId, NewsItemAction.FIELD_EDITED, operator, detail);
    }

    /**
     * 按字段的输出类型校验并规整新值：文本去掉首尾空白；单选、多选只能取选项内的值或资讯上已有的值，多选去重并按选项顺序排列。
     * JSON null 表示清空。
     */
    private JsonNode normalize(NewsAiFieldPO field, JsonNode value, Optional<JsonNode> current) {
        if (value.isNull()) {
            return NullNode.getInstance();
        }
        NewsAiFieldOutputType outputType = field.getOutputType();
        if (outputType == NewsAiFieldOutputType.TEXT) {
            requireText(field, value);
            return TextNode.valueOf(StringUtils.trim(value.asText()));
        }
        if (outputType == NewsAiFieldOutputType.NUMBER) {
            if (!value.isNumber()) {
                throw new BusinessException(field.getName() + "需要填写数字");
            }
            return value;
        }
        List<String> options = NewsAiFieldRules.options(field);
        Set<String> currentValues = current.map(this::textValues).orElseGet(Stream::empty).collect(Collectors.toSet());
        if (outputType == NewsAiFieldOutputType.SINGLE) {
            requireText(field, value);
            String picked = StringUtils.trim(value.asText());
            if (StringUtils.isNotEmpty(picked)) {
                requireOption(field, picked, options, currentValues);
            }
            return TextNode.valueOf(picked);
        }
        if (!value.isArray()) {
            throw new BusinessException(field.getName() + "需要选择若干项");
        }
        Set<String> picked = new LinkedHashSet<>();
        for (JsonNode element : value) {
            requireText(field, element);
            String option = StringUtils.trim(element.asText());
            requireOption(field, option, options, currentValues);
            picked.add(option);
        }
        ArrayNode normalized = JsonNodeFactory.instance.arrayNode();
        options.stream().filter(picked::contains).forEach(normalized::add);
        picked.stream().filter(option -> !options.contains(option)).forEach(normalized::add);
        return normalized;
    }

    private void requireText(NewsAiFieldPO field, JsonNode value) {
        if (!value.isTextual()) {
            throw new BusinessException(field.getName() + "需要填写文本");
        }
    }

    /**
     * 选项值必须在字段选项内；资讯上已有的值（如选项调整前打上的标签）可以保留。
     */
    private void requireOption(NewsAiFieldPO field, String value, List<String> options, Set<String> currentValues) {
        if (!options.contains(value) && !currentValues.contains(value)) {
            throw new BusinessException(field.getName() + "没有选项「" + value + "」");
        }
    }

    /**
     * 两个值是否相同：多选按集合比较，其他按 JSON 值比较。
     */
    private boolean sameValue(NewsAiFieldPO field, JsonNode current, JsonNode value) {
        if (field.getOutputType() == NewsAiFieldOutputType.MULTI && current.isArray() && value.isArray()) {
            return textValues(current).collect(Collectors.toSet()).equals(textValues(value).collect(Collectors.toSet()));
        }
        return current.equals(value);
    }

    private Stream<String> textValues(JsonNode value) {
        Stream<JsonNode> elements = value.isArray()
                ? StreamSupport.stream(value.spliterator(), false)
                : Stream.of(value);
        return elements.filter(JsonNode::isTextual).map(JsonNode::asText);
    }
}
