package com.zengame.ai.business.news.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资讯内容类型（news_feed.content_type、news_item.content_type），也是资讯库的一级分类。
 */
@Getter
@AllArgsConstructor
public enum NewsContentType implements CodeEnum {

    ARTICLE("文章"),

    VIDEO("视频");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的内容类型
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NewsContentType fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsContentType.class);
    }
}
