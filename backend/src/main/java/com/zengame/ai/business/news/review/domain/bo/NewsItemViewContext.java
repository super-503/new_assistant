package com.zengame.ai.business.news.review.domain.bo;

import com.zengame.ai.business.news.domain.enums.NewsAiFieldKind;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.review.NewsAiFieldRules;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import com.zengame.ai.repository.ai.po.NewsChannelPO;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import com.zengame.ai.repository.ai.po.NewsRejectReasonPO;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 渲染资讯卡片和详情所需的配置快照：AI 字段、渠道、订阅源、拒绝理由。一次请求内加载一次。
 */
@Getter
public class NewsItemViewContext {

    private final boolean modelConfigured;

    /**
     * 当前可用且已启用的 AI 字段，按展示顺序
     */
    private final List<NewsAiFieldPO> enabledFields;

    /**
     * 全部渠道，按主键升序
     */
    private final List<NewsChannelPO> channels;

    /**
     * 现存的全部订阅源，按渠道、主键升序
     */
    private final List<NewsFeedPO> feeds;

    private final Map<String, NewsChannelPO> channelByCode;

    private final Map<Long, NewsFeedPO> feedById;

    private final Map<Long, NewsRejectReasonPO> reasonById;

    private final Map<String, NewsAiFieldPO> fieldByKey;

    /**
     * @param allFields 全部 AI 字段，按展示顺序，含历史记录使用的字段名称
     * @param available 当前可用的字段
     * @param modelConfigured 是否已配置 AI 模型
     * @param channels  全部渠道
     * @param feeds     现存的全部订阅源
     * @param reasons   全部拒绝理由
     */
    public NewsItemViewContext(List<NewsAiFieldPO> allFields, Predicate<NewsAiFieldPO> available, boolean modelConfigured,
                               List<NewsChannelPO> channels,
                               List<NewsFeedPO> feeds, List<NewsRejectReasonPO> reasons) {
        this.modelConfigured = modelConfigured;
        this.enabledFields = allFields.stream()
                .filter(field -> modelConfigured)
                .filter(NewsAiFieldPO::getEnabled)
                .filter(available)
                .collect(Collectors.toList());
        this.channels = channels;
        this.feeds = feeds;
        this.channelByCode = channels.stream().collect(Collectors.toMap(NewsChannelPO::getCode, Function.identity()));
        this.feedById = feeds.stream().collect(Collectors.toMap(NewsFeedPO::getId, Function.identity()));
        this.reasonById = reasons.stream().collect(Collectors.toMap(NewsRejectReasonPO::getId, Function.identity()));
        this.fieldByKey = allFields.stream().collect(Collectors.toMap(NewsAiFieldPO::getFieldKey, Function.identity()));
    }

    /**
     * @param contentType 资讯类型
     * @return 对该类型适用的启用字段，按展示顺序
     */
    public List<NewsAiFieldPO> applicableFields(NewsContentType contentType) {
        return enabledFields.stream()
                .filter(field -> NewsAiFieldRules.isApplicable(field, contentType))
                .collect(Collectors.toList());
    }

    /**
     * @return 启用中的标签字段：先是普通标签字段，再是岗位分类，同类按展示顺序
     */
    public List<NewsAiFieldPO> tagFields() {
        return enabledFields.stream()
                .filter(NewsAiFieldRules::isTagField)
                .sorted(Comparator.comparing(field -> field.getKind() == NewsAiFieldKind.BUILTIN))
                .collect(Collectors.toList());
    }

    /**
     * @param contentType 资讯类型
     * @return 对该类型适用的启用标签字段，顺序同 {@link #tagFields()}
     */
    public List<NewsAiFieldPO> applicableTagFields(NewsContentType contentType) {
        return tagFields().stream()
                .filter(field -> NewsAiFieldRules.isApplicable(field, contentType))
                .collect(Collectors.toList());
    }

    /**
     * @param channelCode 渠道标识
     * @return 渠道名称
     */
    public String channelName(String channelCode) {
        return Optional.ofNullable(channelByCode.get(channelCode))
                .map(NewsChannelPO::getName)
                .orElseThrow(() -> new IllegalStateException("渠道不存在: " + channelCode));
    }

    /**
     * @param feedId 订阅源主键
     * @return 订阅源；已删除时为空
     */
    public Optional<NewsFeedPO> feed(Long feedId) {
        return Optional.ofNullable(feedById.get(feedId));
    }

    /**
     * @param reasonId 拒绝理由主键
     * @return 拒绝理由名称；没有拒绝理由时为空
     */
    public Optional<String> reasonName(Long reasonId) {
        return Optional.ofNullable(reasonId)
                .map(id -> Optional.ofNullable(reasonById.get(id))
                        .orElseThrow(() -> new IllegalStateException("拒绝理由不存在: " + id)))
                .map(NewsRejectReasonPO::getName);
    }

    /**
     * @param fieldKey 字段标识
     * @return 字段名称；字段已删除时为字段标识
     */
    public String fieldName(String fieldKey) {
        return Optional.ofNullable(fieldByKey.get(fieldKey))
                .map(NewsAiFieldPO::getName)
                .orElse(fieldKey);
    }
}
