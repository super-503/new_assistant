package com.zengame.ai.business.news.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订阅源最近一次拉取的结果（news_feed.pull_status），只反映订阅源自己的拉取结果。
 */
@Getter
@AllArgsConstructor
public enum NewsFeedPullStatus implements CodeEnum {

    /** 新建后还没有拉取过 */
    PENDING("待首次拉取"),

    OK("正常"),

    /** 拉取失败，原因记在 pull_error */
    ERROR("拉取失败");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的拉取状态
     */
    @JsonCreator
    public static NewsFeedPullStatus fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsFeedPullStatus.class);
    }
}
