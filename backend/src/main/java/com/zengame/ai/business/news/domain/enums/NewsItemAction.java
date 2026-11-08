package com.zengame.ai.business.news.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.zengame.ai.infrastructure.enums.CodeEnum;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资讯处理记录的动作（news_item_log.action），组成单条资讯的时间线。
 */
@Getter
@AllArgsConstructor
public enum NewsItemAction implements CodeEnum {

    COLLECTED("采集入库"),

    /** 结构化完成，所有适用字段都已生成 */
    PROCESSED("结构化完成"),

    /** 结构化失败或部分字段未生成 */
    PROCESS_FAILED("结构化失败"),

    FIELD_EDITED("修改字段"),

    APPROVED("审核通过"),

    REJECTED("审核拒绝"),

    /** 已拒绝的资讯恢复到待审 */
    RESTORED("恢复到待审");

    private final String desc;

    @Override
    @JsonValue
    public String getCode() {
        return name();
    }

    /**
     * @param code 枚举编码
     * @return 编码对应的处理动作
     */
    @JsonCreator
    public static NewsItemAction fromCode(String code) {
        return CodeEnumUtils.fromCode(code, NewsItemAction.class);
    }
}
