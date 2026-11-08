package com.zengame.ai.business.news.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 渠道凭证状态（news_channel.credential_status）。
 */
@Getter
@AllArgsConstructor
public enum NewsCredentialStatus implements CodeEnum {

    /** 渠道的凭证方式为无凭证 */
    NONE("无需凭证"),

    /** 凭证可用，渠道正常拉取 */
    VALID("有效"),

    /** 未登录或登录已失效，定时拉取跳过该渠道 */
    INVALID("已失效");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的凭证状态
     */
    @JsonCreator
    public static NewsCredentialStatus fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsCredentialStatus.class);
    }
}
