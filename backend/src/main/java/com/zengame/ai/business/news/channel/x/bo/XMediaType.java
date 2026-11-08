package com.zengame.ai.business.news.channel.x.bo;

import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 推文媒体类型，编码为 X 接口 extended_entities.media[].type 的取值。
 */
@Getter
@AllArgsConstructor
public enum XMediaType implements CodeEnum {

    PHOTO("photo"),

    VIDEO("video"),

    ANIMATED_GIF("animated_gif");

    private final String code;

    /**
     * @param code X 接口的媒体类型
     * @return 媒体类型
     */
    public static XMediaType fromCode(String code) {
        return CodeEnumUtils.fromCode(code, XMediaType.class);
    }
}
