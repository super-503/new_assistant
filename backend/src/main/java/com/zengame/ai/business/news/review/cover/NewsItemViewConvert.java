package com.zengame.ai.business.news.review.cover;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.zengame.ai.business.news.channel.bo.Playback;
import com.zengame.ai.business.news.channel.bo.StatItem;
import com.zengame.ai.business.news.domain.enums.NewsAiFieldKind;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.processing.StringListJson;
import com.zengame.ai.business.news.review.NewsAiFieldRules;
import com.zengame.ai.business.news.review.domain.bo.NewsItemBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemLogDetailBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemViewContext;
import com.zengame.ai.business.news.review.domain.vo.NewsFeedOptionVO;
import com.zengame.ai.business.news.review.domain.vo.NewsFieldOptionVO;
import com.zengame.ai.business.news.review.domain.vo.NewsFieldRefVO;
import com.zengame.ai.business.news.review.domain.vo.NewsItemCardVO;
import com.zengame.ai.business.news.review.domain.vo.NewsItemDetailVO;
import com.zengame.ai.business.news.review.domain.vo.NewsItemFieldVO;
import com.zengame.ai.business.news.review.domain.vo.NewsItemLogVO;
import com.zengame.ai.business.news.review.domain.vo.NewsItemTagVO;
import com.zengame.ai.business.news.review.domain.vo.NewsLogLineVO;
import com.zengame.ai.business.news.review.domain.vo.NewsRejectReasonOptionVO;
import com.zengame.ai.business.news.review.domain.vo.NewsTagFieldVO;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import com.zengame.ai.repository.ai.po.NewsItemLogPO;
import com.zengame.ai.repository.ai.po.NewsRejectReasonPO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 资讯到列表项、详情、处理记录，以及筛选项的展示映射。派生字段依赖 {@link NewsItemViewContext} 中的配置快照。
 */
@Mapper
public interface NewsItemViewConvert {

    NewsItemViewConvert INSTANCE = Mappers.getMapper(NewsItemViewConvert.class);

    /**
     * 视频简介取正文纯文本的字数
     */
    int VIDEO_SUMMARY_LENGTH = 120;

    /**
     * 处理记录里系统动作的操作人
     */
    String SYSTEM_OPERATOR = "系统";

    /**
     * 多个字段名之间的分隔
     */
    String NAME_DELIMITER = "、";

    /**
     * 岗位分类选中全部选项时展示的标签值
     */
    String ALL_JOB_FAMILY_TAG = "全部";

    @Mapping(target = "title", source = "item", qualifiedByName = "title")
    @Mapping(target = "summary", source = "item", qualifiedByName = "summary")
    @Mapping(target = "channelName", source = "channelCode", qualifiedByName = "channelName")
    @Mapping(target = "feedName", source = "feedId", qualifiedByName = "feedName")
    @Mapping(target = "feedAvatarUrl", source = "feedId", qualifiedByName = "feedAvatarUrl")
    @Mapping(target = "url", source = "canonicalUrl")
    @Mapping(target = "tags", source = "item", qualifiedByName = "tags")
    @Mapping(target = "missingFields", source = "item", qualifiedByName = "missingFields")
    @Mapping(target = "rejectReasonName", source = "rejectReasonId", qualifiedByName = "rejectReasonName")
    NewsItemCardVO toCard(NewsItemBO item, @Context NewsItemViewContext context);

    @Mapping(target = "title", source = "item", qualifiedByName = "title")
    @Mapping(target = "summary", source = "item", qualifiedByName = "summary")
    @Mapping(target = "contentTypeName", source = "item.contentType.desc")
    @Mapping(target = "statusName", source = "item.status.desc")
    @Mapping(target = "channelName", source = "item.channelCode", qualifiedByName = "channelName")
    @Mapping(target = "feedName", source = "item.feedId", qualifiedByName = "feedName")
    @Mapping(target = "feedAvatarUrl", source = "item.feedId", qualifiedByName = "feedAvatarUrl")
    @Mapping(target = "url", source = "item.canonicalUrl")
    @Mapping(target = "playback", source = "item.playbackJson", qualifiedByName = "playback")
    @Mapping(target = "stats", source = "item.statsJson", qualifiedByName = "stats")
    @Mapping(target = "tags", source = "item", qualifiedByName = "tags")
    @Mapping(target = "missingFields", source = "item", qualifiedByName = "missingFields")
    @Mapping(target = "fields", source = "item", qualifiedByName = "fields")
    @Mapping(target = "rejectReasonName", source = "item.rejectReasonId", qualifiedByName = "rejectReasonName")
    @Mapping(target = "logs", source = "logs", qualifiedByName = "logs")
    NewsItemDetailVO toDetail(NewsItemBO item, List<NewsItemLogPO> logs, @Context NewsItemViewContext context);

    @Mapping(target = "options", source = "field", qualifiedByName = "fieldOptions")
    @Mapping(target = "value", source = "fieldKey", qualifiedByName = "fieldValue")
    @Mapping(target = "edited", source = "fieldKey", qualifiedByName = "fieldEdited")
    @Mapping(target = "missing", source = "field", qualifiedByName = "fieldMissing")
    NewsItemFieldVO toField(NewsAiFieldPO field, @Context NewsItemBO item);

    @Mapping(target = "values", source = "field", qualifiedByName = "tagValues")
    NewsItemTagVO toTag(NewsAiFieldPO field, @Context NewsItemBO item);

    @Mapping(target = "time", source = "createTime")
    @Mapping(target = "actionName", source = "action.desc")
    @Mapping(target = "operator", source = "operator", qualifiedByName = "operatorName")
    @Mapping(target = "lines", source = "log", qualifiedByName = "logLines")
    NewsItemLogVO toLog(NewsItemLogPO log, @Context NewsItemViewContext context);

    @Mapping(target = "options", source = "field", qualifiedByName = "fieldOptions")
    NewsTagFieldVO toTagField(NewsAiFieldPO field);

    List<NewsTagFieldVO> toTagFields(List<NewsAiFieldPO> fields);

    @Mapping(target = "feedId", source = "id")
    @Mapping(target = "feedName", source = "displayName")
    NewsFeedOptionVO toFeedOption(NewsFeedPO feed);

    List<NewsFeedOptionVO> toFeedOptions(List<NewsFeedPO> feeds);

    NewsRejectReasonOptionVO toReasonOption(NewsRejectReasonPO reason);

    List<NewsRejectReasonOptionVO> toReasonOptions(List<NewsRejectReasonPO> reasons);

    /**
     * @param item 资讯
     * @param context 配置快照
     * @return 展示标题
     */
    @Named("title")
    default String title(NewsItemBO item, @Context NewsItemViewContext context) {
        return item.displayTitle(context.isModelConfigured());
    }

    /**
     * @param item 资讯
     * @param context 配置快照
     * @return 文章取 AI 字段「摘要」，视频取正文纯文本前 120 字；没有时为 null
     */
    @Named("summary")
    default String summary(NewsItemBO item, @Context NewsItemViewContext context) {
        if (item.getContentType() == NewsContentType.VIDEO) {
            return Optional.ofNullable(item.getBodyHtml())
                    .map(html -> Jsoup.parse(html).text())
                    .filter(StringUtils::isNotBlank)
                    .map(text -> StringUtils.left(text, VIDEO_SUMMARY_LENGTH))
                    .orElse(null);
        }
        if (!context.isModelConfigured()) {
            return null;
        }
        return item.text(NewsItemBO.SUMMARY_KEY).orElse(null);
    }

    /**
     * @param channelCode 渠道标识
     * @param context     配置快照
     * @return 渠道名称
     */
    @Named("channelName")
    default String channelName(String channelCode, @Context NewsItemViewContext context) {
        return context.channelName(channelCode);
    }

    /**
     * @param feedId  订阅源主键
     * @param context 配置快照
     * @return 订阅源显示名称；已删除时为 null
     */
    @Named("feedName")
    default String feedName(Long feedId, @Context NewsItemViewContext context) {
        return context.feed(feedId).map(NewsFeedPO::getDisplayName).orElse(null);
    }

    /**
     * @param feedId  订阅源主键
     * @param context 配置快照
     * @return 订阅源头像；已删除或没有头像时为 null
     */
    @Named("feedAvatarUrl")
    default String feedAvatarUrl(Long feedId, @Context NewsItemViewContext context) {
        return context.feed(feedId).map(NewsFeedPO::getAvatarUrl).orElse(null);
    }

    /**
     * @param rejectReasonId 拒绝理由主键，可为空
     * @param context        配置快照
     * @return 拒绝理由名称；未拒绝时为 null
     */
    @Named("rejectReasonName")
    default String rejectReasonName(Long rejectReasonId, @Context NewsItemViewContext context) {
        return context.reasonName(rejectReasonId).orElse(null);
    }

    /**
     * @param playbackJson 播放方式 JSON，可为空
     * @return 播放方式；没有时为 null
     */
    @Named("playback")
    default Playback playback(String playbackJson) {
        return Optional.ofNullable(playbackJson)
                .map(json -> JacksonUtil.readValue(json, Playback.class))
                .orElse(null);
    }

    /**
     * @param statsJson 统计项 JSON 数组
     * @return 统计项
     */
    @Named("stats")
    default List<StatItem> stats(String statsJson) {
        return JacksonUtil.readValue(statsJson, new TypeReference<List<StatItem>>() {
        });
    }

    /**
     * @param item    资讯
     * @param context 配置快照
     * @return 有值的标签字段：先是普通标签字段，再是岗位分类
     */
    @Named("tags")
    default List<NewsItemTagVO> tags(NewsItemBO item, @Context NewsItemViewContext context) {
        return context.applicableTagFields(item.getContentType()).stream()
                .map(field -> toTag(field, item))
                .filter(tag -> CollectionUtils.isNotEmpty(tag.getValues()))
                .collect(Collectors.toList());
    }

    /**
     * @param item    资讯
     * @param context 配置快照
     * @return 未生成的字段，按展示顺序
     */
    @Named("missingFields")
    default List<NewsFieldRefVO> missingFields(NewsItemBO item, @Context NewsItemViewContext context) {
        return context.applicableFields(item.getContentType()).stream()
                .filter(field -> NewsAiFieldRules.isMissing(field, item))
                .map(field -> new NewsFieldRefVO(field.getFieldKey(), field.getName()))
                .collect(Collectors.toList());
    }

    /**
     * @param item    资讯
     * @param context 配置快照
     * @return 对这条资讯适用的启用字段，按展示顺序
     */
    @Named("fields")
    default List<NewsItemFieldVO> fields(NewsItemBO item, @Context NewsItemViewContext context) {
        return context.applicableFields(item.getContentType()).stream()
                .map(field -> toField(field, item))
                .collect(Collectors.toList());
    }

    /**
     * @param logs    处理记录，按发生先后升序
     * @param context 配置快照
     * @return 处理记录，最近的在前
     */
    @Named("logs")
    default List<NewsItemLogVO> logs(List<NewsItemLogPO> logs, @Context NewsItemViewContext context) {
        List<NewsItemLogPO> latestFirst = new ArrayList<>(logs);
        Collections.reverse(latestFirst);
        return latestFirst.stream().map(log -> toLog(log, context)).collect(Collectors.toList());
    }

    /**
     * @param field AI 字段
     * @return 单选、多选的选项及配色；其他类型为空列表
     */
    @Named("fieldOptions")
    default List<NewsFieldOptionVO> fieldOptions(NewsAiFieldPO field) {
        if (!NewsAiFieldRules.isChoice(field)) {
            return Collections.emptyList();
        }
        return NewsAiFieldRules.options(field).stream()
                .map(option -> new NewsFieldOptionVO(option, NewsAiFieldRules.colorIndex(field, option).orElse(null)))
                .collect(Collectors.toList());
    }

    /**
     * @param fieldKey 字段标识
     * @param item     资讯
     * @return 字段当前值；没有时为 null
     */
    @Named("fieldValue")
    default JsonNode fieldValue(String fieldKey, @Context NewsItemBO item) {
        return item.value(fieldKey).orElse(null);
    }

    /**
     * @param fieldKey 字段标识
     * @param item     资讯
     * @return 人工修改过返回 true
     */
    @Named("fieldEdited")
    default boolean fieldEdited(String fieldKey, @Context NewsItemBO item) {
        return item.isEdited(fieldKey);
    }

    /**
     * @param field AI 字段
     * @param item  资讯
     * @return 未生成返回 true
     */
    @Named("fieldMissing")
    default boolean fieldMissing(NewsAiFieldPO field, @Context NewsItemBO item) {
        return NewsAiFieldRules.isMissing(field, item);
    }

    /**
     * @param field 标签字段
     * @param item  资讯
     * @return 字段当前值及配色；没有值时为空列表
     */
    @Named("tagValues")
    default List<NewsFieldOptionVO> tagValues(NewsAiFieldPO field, @Context NewsItemBO item) {
        List<NewsFieldOptionVO> values = item.value(field.getFieldKey())
                .map(value -> value.isArray()
                        ? StreamSupport.stream(value.spliterator(), false)
                        : Stream.of(value))
                .orElseGet(Stream::empty)
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .filter(StringUtils::isNotBlank)
                .map(value -> new NewsFieldOptionVO(value, NewsAiFieldRules.colorIndex(field, value).orElse(null)))
                .collect(Collectors.toList());
        return allOptionsSelected(field, values) ? List.of(new NewsFieldOptionVO(ALL_JOB_FAMILY_TAG, null)) : values;
    }

    /**
     * 岗位分类把所有选项都打上时，标签合并成一个「全部」。
     *
     * @param field  AI 字段
     * @param values 字段当前值
     * @return 覆盖全部选项返回 true
     */
    private boolean allOptionsSelected(NewsAiFieldPO field, List<NewsFieldOptionVO> values) {
        if (field.getKind() != NewsAiFieldKind.BUILTIN) {
            return false;
        }
        List<String> options = StringListJson.read(field.getOptionsJson());
        if (options.size() < 2) {
            return false;
        }
        Set<String> selected = values.stream().map(NewsFieldOptionVO::getValue).collect(Collectors.toSet());
        return selected.containsAll(options);
    }

    /**
     * @param operator 操作人登录名，系统动作为空
     * @return 操作人；系统动作为「系统」
     */
    @Named("operatorName")
    default String operatorName(String operator) {
        return Optional.ofNullable(operator).filter(StringUtils::isNotBlank).orElse(SYSTEM_OPERATOR);
    }

    /**
     * 处理记录的附加信息：拒绝理由和补充说明、修改的字段、未生成的字段和失败原因。
     *
     * @param log     处理记录
     * @param context 配置快照
     * @return 附加信息；没有时为空列表
     */
    @Named("logLines")
    default List<NewsLogLineVO> logLines(NewsItemLogPO log, @Context NewsItemViewContext context) {
        if (StringUtils.isBlank(log.getDetailJson())) {
            return Collections.emptyList();
        }
        NewsItemLogDetailBO detail = JacksonUtil.readValue(log.getDetailJson(), NewsItemLogDetailBO.class);
        List<NewsLogLineVO> lines = new ArrayList<>();
        switch (log.getAction()) {
            case REJECTED:
                lines.add(new NewsLogLineVO("拒绝理由", detail.getReasonName(), true));
                Optional.ofNullable(detail.getNote())
                        .filter(StringUtils::isNotBlank)
                        .ifPresent(note -> lines.add(new NewsLogLineVO("补充说明", note, false)));
                break;
            case FIELD_EDITED:
                lines.add(new NewsLogLineVO("修改字段", fieldNames(detail.getFieldKeys(), context), false));
                break;
            case PROCESS_FAILED:
                Optional.ofNullable(detail.getFailedKeys())
                        .filter(CollectionUtils::isNotEmpty)
                        .ifPresent(keys -> lines.add(new NewsLogLineVO("未生成字段", fieldNames(keys, context), true)));
                Optional.ofNullable(detail.getError())
                        .filter(StringUtils::isNotBlank)
                        .ifPresent(error -> lines.add(new NewsLogLineVO("失败原因", error, false)));
                break;
            default:
                break;
        }
        return lines;
    }

    /**
     * @param fieldKeys 字段标识
     * @param context   配置快照
     * @return 字段名称，以「、」连接
     */
    @Named("fieldNames")
    default String fieldNames(List<String> fieldKeys, NewsItemViewContext context) {
        return fieldKeys.stream().map(context::fieldName).collect(Collectors.joining(NAME_DELIMITER));
    }
}
