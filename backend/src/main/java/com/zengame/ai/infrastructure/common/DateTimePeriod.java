package com.zengame.ai.infrastructure.common;

import lombok.Value;

import java.time.LocalDateTime;

/**
 * 日期段
 *
 * @author john
 * @since 2021/5/13
 */
@Value
public class DateTimePeriod {

    /**
     * 开始日期，包含当天
     */
    LocalDateTime begin;

    /**
     * 结束日期，包含当天
     */
    LocalDateTime end;
}
