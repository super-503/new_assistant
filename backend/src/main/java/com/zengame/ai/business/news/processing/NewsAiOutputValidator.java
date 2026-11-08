package com.zengame.ai.business.news.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.zengame.ai.business.news.processing.domain.bo.NewsAiGeneration;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 数据加工的结果校验：把模型输出解析为 JSON 对象，按字段的输出类型和选项逐个校验。
 *
 * <p>合法值的存储格式：文本为去掉首尾空白的字符串；数字为 JSON 数字；单选为选项字符串；多选为去重后的选项字符串数组，可以为空数组。
 * 非必填文本允许留空，模型给出空白文本时存为空字符串，不计为未生成。
 */
@Component
public class NewsAiOutputValidator {

    private static final int CONTENT_ABBREVIATE_LENGTH = 200;

    /**
     * 校验一次生成的输出。
     *
     * @param fields  参与生成的字段
     * @param content 模型输出
     * @return 合法的字段值和未生成的字段标识
     * @throws IllegalStateException 输出里没有 JSON 对象
     */
    public NewsAiGeneration validate(List<NewsAiFieldPO> fields, String content) {
        JsonNode root = readObject(content);
        ObjectNode values = JsonNodeFactory.instance.objectNode();
        List<String> failedKeys = new ArrayList<>();
        for (NewsAiFieldPO field : fields) {
            Optional<JsonNode> value = normalize(field, root.get(field.getFieldKey()));
            if (value.isPresent()) {
                values.set(field.getFieldKey(), value.get());
            } else {
                failedKeys.add(field.getFieldKey());
            }
        }
        return new NewsAiGeneration(values, failedKeys);
    }

    /**
     * 按输出类型校验并规整一个字段的值。
     *
     * @param field AI 字段
     * @param raw   模型给出的值；缺失时为 null
     * @return 合法时为规整后的值；缺失、类型不符、取值在选项外、必填文本为空白时为空
     */
    Optional<JsonNode> normalize(NewsAiFieldPO field, JsonNode raw) {
        if (raw == null || raw.isNull()) {
            return Optional.empty();
        }
        switch (field.getOutputType()) {
            case TEXT:
                return raw.isTextual() ? textValue(raw.asText(), field) : Optional.empty();
            case NUMBER:
                return Optional.of(raw).filter(JsonNode::isNumber);
            case SINGLE:
                return textOf(raw)
                        .filter(StringListJson.read(field.getOptionsJson())::contains)
                        .map(TextNode::valueOf);
            case MULTI:
                return multiOf(raw, StringListJson.read(field.getOptionsJson()));
            default:
                throw new IllegalStateException("未知的输出类型: " + field.getOutputType());
        }
    }

    /**
     * 文本值：去掉首尾空白；非必填字段的空白文本存为空字符串，必填字段的空白仍算未生成。
     */
    private static Optional<JsonNode> textValue(String text, NewsAiFieldPO field) {
        String trimmed = text.trim();
        if (!trimmed.isEmpty()) {
            return Optional.of(TextNode.valueOf(trimmed));
        }
        return BooleanUtils.isNotTrue(field.getRequired())
                ? Optional.of(TextNode.valueOf(StringUtils.EMPTY))
                : Optional.empty();
    }

    /**
     * 多选值：数组里每个元素都必须是选项，任一元素不合法则整个字段不合法。
     */
    private Optional<JsonNode> multiOf(JsonNode raw, List<String> options) {
        if (!raw.isArray()) {
            return Optional.empty();
        }
        Set<String> selected = new LinkedHashSet<>();
        for (JsonNode element : raw) {
            Optional<String> option = textOf(element).filter(options::contains);
            if (option.isEmpty()) {
                return Optional.empty();
            }
            selected.add(option.get());
        }
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        selected.forEach(array::add);
        return Optional.of(array);
    }

    /**
     * @return 非空白字符串去掉首尾空白；其他类型或空白时为空
     */
    private Optional<String> textOf(JsonNode node) {
        return Optional.of(node)
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .map(StringUtils::trim)
                .filter(StringUtils::isNotEmpty);
    }

    /**
     * 解析模型输出里的 JSON 对象：模型可能用 Markdown 代码块包裹，取第一个 { 到最后一个 } 之间的内容。
     */
    private JsonNode readObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalStateException("模型输出不是 JSON 对象: " + StringUtils.abbreviate(content, CONTENT_ABBREVIATE_LENGTH));
        }
        JsonNode root = JacksonUtil.readTree(content.substring(start, end + 1));
        if (!root.isObject()) {
            throw new IllegalStateException("模型输出不是 JSON 对象: " + StringUtils.abbreviate(content, CONTENT_ABBREVIATE_LENGTH));
        }
        return root;
    }
}
