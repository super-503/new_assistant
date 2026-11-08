package com.zengame.ai.business.news.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 渠道运行的触发方式（news_channel_run.trigger_type）。
 */
@Getter
@AllArgsConstructor
public enum NewsRunTriggerType implements CodeEnum {

    /** xxl-job 定时拉取全部渠道 */
    SCHEDULE("定时"),

    /** 订阅源的立即拉取或重试 */
    MANUAL("手动");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的触发方式
     */
    @JsonCreator
    public static NewsRunTriggerType fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsRunTriggerType.class);
    }
}
