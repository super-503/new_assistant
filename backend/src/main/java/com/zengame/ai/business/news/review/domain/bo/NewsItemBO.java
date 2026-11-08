package com.zengame.ai.business.news.review.domain.bo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 审核与资讯库使用的资讯：在 news_item 行的基础上，把 AI 结果、未生成字段、已修改字段解析为结构化数据。
 */
@Data
public class NewsItemBO {

    /**
     * AI 字段「标题」的字段标识
     */
    public static final String TITLE_KEY = "title";

    /**
     * AI 字段「摘要」的字段标识
     */
    public static final String SUMMARY_KEY = "summary";

    private Long id;

    private Long feedId;

    private String channelCode;

    private String canonicalUrl;

    private NewsContentType contentType;

    private String originTitle;

    private String author;

    private String authorAvatarUrl;

    private LocalDateTime publishTime;

    private String coverUrl;

    private String bodyHtml;

    /**
     * 站内播放方式 JSON，结构为 {@code Playback}；没有时为空
     */
    private String playbackJson;

    /**
     * 统计项 JSON 数组，元素结构为 {@code StatItem}
     */
    private String statsJson;

    private Integer durationSeconds;

    private NewsItemStatus status;

    /**
     * AI 字段结果：{字段标识: 值}。文本、单选为字符串，数字为数值，多选为字符串数组
     */
    private ObjectNode aiValues;

    /**
     * 未生成的字段标识
     */
    private List<String> aiFailedKeys;

    /**
     * 人工修改过的字段标识
     */
    private List<String> editedKeys;

    private Long rejectReasonId;

    private String rejectNote;

    private String reviewer;

    private LocalDateTime reviewTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * @param fieldKey 字段标识
     * @return 字段当前值；结果里没有该字段时为空
     */
    public Optional<JsonNode> value(String fieldKey) {
        return Optional.ofNullable(aiValues.get(fieldKey));
    }

    /**
     * 字段未生成：结果里没有该字段，或加工时记为未生成。
     *
     * @param fieldKey 字段标识
     * @return 未生成返回 true
     */
    public boolean isMissing(String fieldKey) {
        return !aiValues.has(fieldKey) || aiFailedKeys.contains(fieldKey);
    }

    /**
     * @param fieldKey 字段标识
     * @return 人工修改过返回 true
     */
    public boolean isEdited(String fieldKey) {
        return editedKeys.contains(fieldKey);
    }

    /**
     * @param fieldKey 字段标识
     * @return 字段的文本值；字段未生成、不是文本或为空白时为空
     */
    public Optional<String> text(String fieldKey) {
        return value(fieldKey)
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .filter(StringUtils::isNotBlank);
    }

    /**
     * @param useAiTitle 是否使用 AI 标题
     * @return 展示标题，未使用 AI 标题时取原文标题
     */
    public String displayTitle(boolean useAiTitle) {
        if (useAiTitle) {
            return text(TITLE_KEY).orElse(originTitle);
        }
        return originTitle;
    }
}
