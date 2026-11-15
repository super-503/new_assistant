package com.zengame.ai.apiclient.feishu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** 飞书日报卡片的消息内容。 */
public final class FeishuDigestCard {
    private static final String CARD_PADDING = "12px 12px 12px 12px";

    private FeishuDigestCard() {
    }

    /**
     * @param title 卡片标题
     * @param color 飞书标题配色
     * @param compact 是否在同一行展示附加字段
     * @param entries 本次发送的资讯
     * @return 飞书发送消息接口使用的卡片 JSON
     */
    public static String render(String title, String color, boolean compact, List<Entry> entries) {
        List<Object> elements = new ArrayList<>();
        for (Entry entry : entries) {
            if (!elements.isEmpty()) elements.add(Map.of("tag", "hr"));
            elements.add(Map.of("tag", "div", "text", plainText(StringUtils.normalizeSpace(entry.getTitle()))));
            String details = details(entry, compact);
            if (StringUtils.isNotBlank(details)) {
                elements.add(Map.of("tag", "div", "text", Map.of("tag", "plain_text",
                        "content", details,
                        "text_size", "notation", "text_color", "grey")));
            }
            if (StringUtils.startsWithAny(entry.getUrl(), "https://", "http://")) {
                elements.add(Map.of("tag", "button", "text", plainText("查看原文"),
                        "type", "default", "size", "medium", "behaviors",
                        List.of(Map.of("type", "open_url", "default_url", entry.getUrl()))));
            }
        }
        return JacksonUtil.writeValueAsString(Map.of(
                "schema", "2.0",
                "header", Map.of("title", plainText(title), "subtitle", plainText(entries.size() + " 条资讯"),
                        "template", color, "padding", CARD_PADDING),
                "body", Map.of("direction", "vertical", "padding", CARD_PADDING,
                        "vertical_spacing", "8px", "elements", elements)
        ));
    }

    /** @return 飞书模板变量对象；格式错误时提示管理员修正 */
    public static ObjectNode parseVariables(String value) {
        try {
            ObjectNode variables = JacksonUtil.getObjectMapper().readerFor(ObjectNode.class)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(value);
            if (variables != null) return variables;
        } catch (JsonProcessingException ignored) {
            // 模板变量必须是 JSON 对象。
        }
        throw new BusinessException("飞书模板变量必须是有效的 JSON 对象");
    }

    /**
     * @param id 已发布的卡片模板 ID
     * @param version 模板版本；空值使用飞书上的最新版本
     * @param variables 飞书模板声明的变量及其配置值
     * @param title 本次日报的标题
     * @param createdAt 生成卡片时的 ISO 8601 时间
     * @param compact 汇总文本的字段排版
     * @param entries 本次推送的资讯
     * @return 飞书消息接口使用的模板 JSON
     */
    public static String renderTemplate(String id, String version, ObjectNode variables, String title, String createdAt,
                                        boolean compact, List<Entry> entries) {
        var factory = JsonNodeFactory.instance;
        var items = factory.arrayNode();
        for (Entry entry : entries) {
            var item = items.addObject().put("title", entry.getTitle());
            entry.getFields().forEach(field -> item.set(field.getKey(), field.getValue()));
            if (StringUtils.isNotBlank(entry.getUrl())) item.put("url", entry.getUrl());
        }
        String content = entries.stream().map(entry -> Stream.of(entry.getTitle(), details(entry, compact), entry.getUrl())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("\n"))).collect(Collectors.joining("\n\n"));
        Map<String, JsonNode> values = Map.of("${title}", factory.textNode(title), "${count}", factory.numberNode(entries.size()),
                "${items}", items, "${content}", factory.textNode(content), "${created_at}", factory.textNode(createdAt));
        var data = factory.objectNode().put("template_id", id);
        if (StringUtils.isNotBlank(version)) data.put("template_version_name", version);
        if (!variables.isEmpty()) data.set("template_variable", resolve(variables, values));
        return JacksonUtil.writeValueAsString(factory.objectNode().put("type", "template").set("data", data));
    }

    /** @return 模板变量树，完整占位符以对应的原始 JSON 类型赋值 */
    private static JsonNode resolve(JsonNode node, Map<String, JsonNode> values) {
        if (node.isObject()) {
            var result = JsonNodeFactory.instance.objectNode();
            node.fields().forEachRemaining(field -> result.set(field.getKey(), resolve(field.getValue(), values)));
            return result;
        }
        if (node.isArray()) {
            var result = JsonNodeFactory.instance.arrayNode();
            node.forEach(value -> result.add(resolve(value, values)));
            return result;
        }
        return node.isTextual() ? values.getOrDefault(node.textValue(), node) : node;
    }

    /** @return 该条资讯配置的辅助字段文本 */
    private static String details(Entry entry, boolean compact) {
        return entry.getFields().stream().map(field -> field.getLabel() + "：" + field.getDisplay())
                .map(value -> compact ? StringUtils.normalizeSpace(value) : value)
                .collect(Collectors.joining(compact ? " · " : "\n"));
    }

    /** @return 飞书卡片中的普通文本节点 */
    private static Map<String, String> plainText(String text) {
        return Map.of("tag", "plain_text", "content", text);
    }

    @Value
    public static class Entry {
        String title;
        String url;
        List<Field> fields;
    }

    @Value
    public static class Field {
        String key;
        String label;
        JsonNode value;
        String display;
    }
}
