package com.zengame.ai.business.news.feed.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订阅源在渠道配置页展示的状态，也是订阅源列表的状态筛选项。
 * 由订阅源的启用状态、拉取结果和所属渠道的凭证状态推导，不落库。
 */
@Getter
@AllArgsConstructor
public enum NewsFeedStatus implements CodeEnum {

    OK("正常"),

    /** 订阅源自己拉取失败，或所属渠道凭证已失效 */
    ERROR("拉取失败"),

    /** 新建后还没有拉取过 */
    PENDING("待首次拉取"),

    /** 订阅源已停用，优先于其他状态 */
    DISABLED("已停用");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的订阅源状态
     */
    @JsonCreator
    public static NewsFeedStatus fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsFeedStatus.class);
    }
}
