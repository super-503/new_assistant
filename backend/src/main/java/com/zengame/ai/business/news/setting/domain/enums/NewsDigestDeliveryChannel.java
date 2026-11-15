package com.zengame.ai.business.news.setting.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;

/** 日报可选择的投递渠道。 */
public enum NewsDigestDeliveryChannel implements CodeEnum {
    FEISHU;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /** @return 请求中选中的日报投递渠道 */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NewsDigestDeliveryChannel fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsDigestDeliveryChannel.class);
    }
}
