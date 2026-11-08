package com.zengame.ai.business.news.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资讯状态（news_item.status）：采集入库后加工中，加工结束进入待审，审核后通过或拒绝。
 */
@Getter
@AllArgsConstructor
public enum NewsItemStatus implements CodeEnum {

    /** 已入库，等待数据加工结束；不出现在审核页 */
    PROCESSING("加工中"),

    /** 加工完成或失败，等待人工审核 */
    PENDING("待审"),

    /** 审核通过，即资讯库内容 */
    APPROVED("已通过"),

    /** 审核拒绝，可恢复到待审 */
    REJECTED("已拒绝");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的资讯状态
     */
    @JsonCreator
    public static NewsItemStatus fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsItemStatus.class);
    }
}
