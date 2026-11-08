package com.zengame.ai.business.news.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 渠道一次运行的结果（news_channel_run.status）。
 */
@Getter
@AllArgsConstructor
public enum NewsRunStatus implements CodeEnum {

    SUCCEEDED("成功"),

    /** 凭证失效、登录失效或有订阅源拉取失败 */
    FAILED("失败");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的运行结果
     */
    @JsonCreator
    public static NewsRunStatus fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsRunStatus.class);
    }
}
