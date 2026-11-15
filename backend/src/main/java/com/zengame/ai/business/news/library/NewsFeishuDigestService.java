package com.zengame.ai.business.news.library;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.zengame.ai.apiclient.feishu.FeishuDigestCard;
import com.zengame.ai.business.news.NewsAiFieldCatalog;
import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import com.zengame.ai.business.news.review.cover.NewsItemConvert;
import com.zengame.ai.business.news.setting.domain.bo.NewsDigestContentSettings;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuCardSettings;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuDigestDelivery;
import com.zengame.ai.business.news.setting.domain.enums.NewsPushPlanBatchResult;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.mapper.NewsFeishuDigestDeliveryMapper;
import com.zengame.ai.repository.ai.mapper.NewsItemMapper;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/** 逐个接收人投递卡片并记录飞书已接收的资讯。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsFeishuDigestService {
    private static final int BATCH_SIZE = 20;
    private static final int PREVIEW_SIZE = 3;
    private static final int MAX_CARD_BYTES = 15_000;
    private static final String AI_FIELD_PREFIX = "ai:";
    private static final DateTimeFormatter PUBLISH_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NewsItemMapper newsItems;
    private final NewsFeishuDigestDeliveryMapper deliveries;
    private final NewsAiFieldCatalog fieldCatalog;

    /**
     * @param card 正在编辑的卡片样式
     * @param content 正在编辑的资讯范围和展示字段；旧配置可为空
     * @return 包含最近通过审核的资讯的飞书卡片 JSON
     * @throws BusinessException 没有可供试发的资讯
     */
    public String previewCardContent(NewsFeishuCardSettings card, NewsDigestContentSettings content) {
        var scope = effectiveContent(card, content);
        var items = newsItems.selectRecentDigestItems(NewsItemStatus.APPROVED, scope, PREVIEW_SIZE);
        if (CollectionUtils.isEmpty(items)) throw new BusinessException("当前推送范围内没有已审核通过的资讯，无法预览");
        var render = cardRenderer(card, scope);
        return buildCard(items, render, aiFieldLabels(scope), scope).getContent();
    }

    /**
     * @param scope 待预览的资讯范围、标题和显示字段
     * @return 最多三条真实已审核资讯，字段值和顺序与推送一致
     */
    public List<FeishuDigestCard.Entry> previewEntries(NewsDigestContentSettings scope) {
        var items = newsItems.selectRecentDigestItems(NewsItemStatus.APPROVED, scope, PREVIEW_SIZE);
        var labels = aiFieldLabels(scope);
        return items.stream().map(item -> toEntry(item, labels, scope)).collect(Collectors.toList());
    }

    /**
     * @param delivery 本次日报的投递目标与卡片样式
     * @return 是否可以继续处理下一批；没有待发资讯或有接收人失败时结束本次任务
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public boolean sendBatch(NewsFeishuDigestDelivery delivery) {
        return sendLegacyBatch(delivery);
    }

    /**
     * @param planId 独立记录投递结果的推送计划
     * @param delivery 本次计划的推送目标和卡片
     * @return 是否继续处理本次计划的下一批资讯
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public boolean sendPlanBatch(long planId, NewsFeishuDigestDelivery delivery) {
        return sendPlanBatchResult(planId, delivery) == NewsPushPlanBatchResult.MORE;
    }

    /**
     * @param planId 独立记录回执的推送计划
     * @param delivery 本次计划的内容和有效接收人
     * @return 继续下一批、投递完成或等待失败补发
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public NewsPushPlanBatchResult sendPlanBatchResult(long planId, NewsFeishuDigestDelivery delivery) {
        if (CollectionUtils.isEmpty(delivery.getTargets())) return NewsPushPlanBatchResult.COMPLETE;
        var scope = effectiveContent(delivery.getCard(), delivery.getContent());
        Function<List<FeishuDigestCard.Entry>, String> render;
        try {
            render = cardRenderer(delivery.getCard(), scope);
        } catch (BusinessException e) {
            log.warn("飞书计划卡片配置无效, planId={}", planId, e);
            return NewsPushPlanBatchResult.RETRY;
        }
        var labels = aiFieldLabels(scope);
        boolean hasItems = false;
        boolean failed = false;
        for (NewsFeishuDigestDelivery.Target target : delivery.getTargets()) {
            var items = newsItems.selectPushPlanRecipientItemsForUpdate(planId, target.getAppId(), target.getOpenId(),
                    NewsItemStatus.APPROVED, scope, BATCH_SIZE);
            if (CollectionUtils.isEmpty(items)) continue;
            hasItems = true;
            var remaining = new ArrayList<>(items);
            try {
                deliver(planId, target, remaining, render, labels, scope);
            } catch (BusinessException e) {
                failed = true;
                log.warn("飞书计划接收人投递失败, planId={}, appId={}, openId={}, pendingCount={}",
                        planId, target.getAppId(), target.getOpenId(), remaining.size(), e);
            }
        }
        deliveries.completePlanItems(planId, delivery.getTargets());
        deliveries.deleteCompletedPlanDelivered(planId);
        if (failed) return NewsPushPlanBatchResult.RETRY;
        if (hasItems) return NewsPushPlanBatchResult.MORE;
        return NewsPushPlanBatchResult.COMPLETE;
    }

    /** @return 旧版日报是否可以继续下一批 */
    private boolean sendLegacyBatch(NewsFeishuDigestDelivery delivery) {
        var scope = effectiveContent(delivery.getCard(), delivery.getContent());
        List<NewsItemPO> items = newsItems.selectFeishuDigestItemsForUpdate(NewsItemStatus.APPROVED, scope, BATCH_SIZE);
        if (CollectionUtils.isEmpty(items)) return false;

        var render = cardRenderer(delivery.getCard(), scope);
        var labels = aiFieldLabels(scope);
        List<Long> itemIds = itemIds(items);
        var pending = new HashSet<Long>();
        for (NewsFeishuDigestDelivery.Target target : delivery.getTargets()) {
            var sent = new HashSet<>(deliveries.selectDelivered(target.getAppId(), target.getOpenId(), itemIds));
            var remaining = items.stream().filter(item -> !sent.contains(item.getId())).collect(Collectors.toList());
            try {
                deliver(null, target, remaining, render, labels, scope);
            } catch (BusinessException e) {
                pending.addAll(itemIds(remaining));
                log.warn("飞书日报接收人投递失败, appId={}, openId={}, pendingCount={}",
                        target.getAppId(), target.getOpenId(), remaining.size(), e);
            }
        }
        var completed = itemIds.stream().filter(id -> !pending.contains(id)).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(completed)) {
            newsItems.markDigestSent(completed);
            deliveries.deleteByItemIds(completed);
        }
        return pending.isEmpty();
    }

    /**
     * @param planId 所属计划，旧版日报为空
     * @param target 应用内的接收人
     * @param remaining 该接收人尚未收到的本批资讯
     * @param render 本批使用的飞书卡片格式
     * @param labels 可展示的 AI 字段名
     * @param scope 已保存的推送内容配置
     */
    private void deliver(Long planId, NewsFeishuDigestDelivery.Target target, List<NewsItemPO> remaining,
                         Function<List<FeishuDigestCard.Entry>, String> render, Map<String, String> labels,
                         NewsDigestContentSettings scope) {
        while (CollectionUtils.isNotEmpty(remaining)) {
            CardBatch batch = buildCard(remaining, render, labels, scope);
            target.getSendCard().accept(batch.getContent());
            var delivered = remaining.subList(0, batch.getItemCount());
            if (planId == null) deliveries.markDelivered(target.getAppId(), target.getOpenId(), itemIds(delivered));
            else deliveries.markPlanDelivered(planId, target.getAppId(), target.getOpenId(), itemIds(delivered));
            delivered.clear();
        }
    }

    /** @return 在飞书消息大小限制内可一次发送的资讯卡片 */
    private CardBatch buildCard(List<NewsItemPO> items, Function<List<FeishuDigestCard.Entry>, String> render,
                                Map<String, String> labels, NewsDigestContentSettings scope) {
        var entries = new ArrayList<FeishuDigestCard.Entry>();
        String content = "";
        for (NewsItemPO item : items) {
            entries.add(toEntry(item, labels, scope));
            String candidate = render.apply(entries);
            if (JacksonUtil.writeValueAsString(candidate).getBytes(StandardCharsets.UTF_8).length > MAX_CARD_BYTES) {
                entries.remove(entries.size() - 1);
                break;
            }
            content = candidate;
        }
        if (entries.isEmpty()) throw new BusinessException("单条日报资讯超出卡片大小限制");
        return new CardBatch(content, entries.size());
    }

    /** @return 日报的飞书原生卡片或已发布模板的内容生成函数 */
    private Function<List<FeishuDigestCard.Entry>, String> cardRenderer(NewsFeishuCardSettings card,
                                                                       NewsDigestContentSettings scope) {
        return Optional.ofNullable(card.getTemplate()).<Function<List<FeishuDigestCard.Entry>, String>>map(template -> {
            var variables = FeishuDigestCard.parseVariables(template.getVariablesJson());
            String createdAt = Instant.now().toString();
            return entries -> FeishuDigestCard.renderTemplate(template.getId(), template.getVersion(), variables,
                    card.getTitle(), createdAt, scope.isCompact(), entries);
        }).orElse(entries -> FeishuDigestCard.render(card.getTitle(), card.getColor().getCode(), scope.isCompact(), entries));
    }

    /** @return 当前推送内容配置，旧配置沿用原卡片的作者选项 */
    private NewsDigestContentSettings effectiveContent(NewsFeishuCardSettings card, NewsDigestContentSettings content) {
        return Optional.ofNullable(content).orElseGet(() -> {
            var defaults = new NewsDigestContentSettings();
            if (!card.isShowAuthor()) defaults.setFields(List.of("url"));
            return defaults;
        });
    }

    /** @return 日报可使用的 AI 字段及其展示名 */
    private Map<String, String> aiFieldLabels(NewsDigestContentSettings scope) {
        if (scope.getFields().stream().noneMatch(key -> key.startsWith(AI_FIELD_PREFIX))) return Map.of();
        return fieldCatalog.listEnabled().stream().collect(Collectors.toMap(NewsAiFieldPO::getFieldKey, NewsAiFieldPO::getName));
    }

    /** @return 一条资讯的标题和按配置顺序展示的附加字段 */
    private FeishuDigestCard.Entry toEntry(NewsItemPO item, Map<String, String> labels, NewsDigestContentSettings scope) {
        ObjectNode aiValues = Optional.ofNullable(item.getAiValuesJson()).filter(StringUtils::isNotBlank)
                .filter(value -> scope.isUseAiTitle() || !labels.isEmpty()).map(NewsItemConvert.INSTANCE::readObject)
                .orElseGet(JsonNodeFactory.instance::objectNode);
        String title = Optional.of(aiValues.path("title")).filter(value -> scope.isUseAiTitle() && value.isTextual())
                .map(JsonNode::textValue).filter(StringUtils::isNotBlank).orElse(item.getOriginTitle());
        var fields = scope.getFields().stream().distinct().map(key -> displayField(key, item, labels, aiValues))
                .flatMap(Optional::stream).collect(Collectors.toList());
        String url = Optional.ofNullable(item.getCanonicalUrl()).filter(value -> scope.getFields().contains("url"))
                .filter(value -> StringUtils.startsWithAny(value, "https://", "http://")).orElse(null);
        return new FeishuDigestCard.Entry(StringUtils.defaultIfBlank(title, "未命名资讯"), url, fields);
    }

    /** @return 资讯字段的文本及飞书模板变量值；没有值的字段不显示 */
    private Optional<FeishuDigestCard.Field> displayField(String key, NewsItemPO item, Map<String, String> labels,
                                                          ObjectNode aiValues) {
        switch (key) {
            case "author":
                return field(key, "作者", TextNode.valueOf(item.getAuthor()));
            case "publishedAt":
                return Optional.ofNullable(item.getPublishTime()).flatMap(time ->
                        field(key, "发布时间", TextNode.valueOf(time.format(PUBLISH_TIME_FORMAT))));
            default:
                return Optional.of(key).filter(value -> value.startsWith(AI_FIELD_PREFIX))
                        .map(value -> value.substring(AI_FIELD_PREFIX.length()))
                        .flatMap(aiKey -> field("ai_" + aiKey, labels.get(aiKey), aiValues.get(aiKey)));
        }
    }

    /** @return 有可展示内容的公开字段 */
    private Optional<FeishuDigestCard.Field> field(String key, String label, JsonNode value) {
        if (StringUtils.isBlank(label) || value == null) return Optional.empty();
        String display = value.asText("");
        if (value.isArray()) {
            display = StreamSupport.stream(value.spliterator(), false).map(item -> item.asText(""))
                    .filter(StringUtils::isNotBlank).collect(Collectors.joining("、"));
        }
        if (StringUtils.isBlank(display)) return Optional.empty();
        return Optional.of(new FeishuDigestCard.Field(key, label, value, display));
    }

    /** @return 本批资讯的持久化主键 */
    private List<Long> itemIds(List<NewsItemPO> items) {
        return items.stream().map(NewsItemPO::getId).collect(Collectors.toList());
    }

    @Value
    private static class CardBatch {
        String content;
        int itemCount;
    }
}
