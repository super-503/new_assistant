package com.zengame.ai.business.news.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 视频的站内播放方式，存于 news_item.playback_json 的 kind。
 */
@Getter
@AllArgsConstructor
public enum NewsPlaybackKind implements CodeEnum {

    /** 内嵌播放器地址，用 iframe 播放 */
    EMBED("内嵌播放器"),

    /** 渠道的视频文件地址和封面 */
    FILE("视频文件");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的播放方式
     */
    @JsonCreator
    public static NewsPlaybackKind fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsPlaybackKind.class);
    }
}
