package com.zengame.ai.business.news.setting.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 日报卡片的标题颜色。 */
@Getter
@AllArgsConstructor
public enum NewsFeishuCardColor implements CodeEnum {
    BLUE("blue", "蓝色"),
    GREEN("green", "绿色"),
    ORANGE("orange", "橙色"),
    PURPLE("purple", "紫色");

    @JsonValue
    private final String code;
    private final String desc;

    /** @return 飞书卡片标题使用的配色 */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NewsFeishuCardColor fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsFeishuCardColor.class);
    }
}
