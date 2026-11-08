package com.zengame.ai.business.news.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 扫码登录一次轮询的结果，由 {@code QrLoginCapable#pollQrLogin} 返回。
 */
@Getter
@AllArgsConstructor
public enum NewsQrLoginStatus implements CodeEnum {

    /** 二维码未扫描 */
    WAITING("等待扫码"),

    /** 已扫码，等待在手机上确认 */
    SCANNED("已扫码"),

    /** 已确认登录，结果里带新凭证 */
    CONFIRMED("登录成功"),

    /** 二维码已过期，需要重新发起 */
    EXPIRED("二维码已过期");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的扫码状态
     */
    @JsonCreator
    public static NewsQrLoginStatus fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsQrLoginStatus.class);
    }
}
