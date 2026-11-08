package com.zengame.ai.business.news.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 渠道的凭证方式，由插件在 {@code ChannelDescriptor} 中声明，决定渠道设置里凭证区的形态。
 */
@Getter
@AllArgsConstructor
public enum NewsCredentialMode implements CodeEnum {

    /** 无需凭证，不显示凭证区 */
    NONE("无凭证"),

    /** 扫码登录，插件实现 {@code QrLoginCapable} */
    QR("扫码"),

    /** 粘贴 Token，插件实现 {@code TokenCredential} */
    TOKEN("Token");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的凭证方式
     */
    @JsonCreator
    public static NewsCredentialMode fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsCredentialMode.class);
    }
}
