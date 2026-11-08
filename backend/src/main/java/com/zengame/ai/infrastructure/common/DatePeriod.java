package com.zengame.ai.infrastructure.common;

import com.zengame.ai.infrastructure.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * 日期段
 *
 * @author john
 * @since 2021/5/13
 */
@Value
public class DatePeriod {

    /**
     * 开始日期，包含当天
     */
    @NotNull
    LocalDate begin;

    /**
     * 结束日期，包含当天
     */
    @NotNull
    LocalDate end;

    /**
     * JSON 反序列化入口
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 日期段
     */
    @JsonCreator
    public static DatePeriod of(@JsonProperty("begin") LocalDate begin, @JsonProperty("end") LocalDate end) {
        return new DatePeriod(begin, end);
    }

    /**
     * 由 [起始日, 结束日] 构造日期区间。
     *
     * @param range 起止两个日期
     * @return 日期区间
     * @throws BusinessException 不是起止两个有效日期
     */
    public static DatePeriod of(List<LocalDate> range) {
        if (range == null || range.size() != 2 || range.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException("period 必须包含起止两个日期");
        }
        return new DatePeriod(range.get(0), range.get(1));
    }

    /**
     * 由可选的 [起始日, 结束日] 构造日期区间，用于「不传表示不限」的查询条件。
     *
     * @param range 起止两个日期，可为空
     * @return 日期区间；range 缺失或不足两个日期时为 null
     */
    public static DatePeriod ofNullable(List<LocalDate> range) {
        if (range == null || range.size() < 2) {
            return null;
        }
        return new DatePeriod(range.get(0), range.get(1));
    }

    public DateTimePeriod toDateTimePeriod() {
        if (begin == null || end == null) {
            return null;
        }
        // 末端取当天最后一微秒（23:59:59.999999）
        // 因为 MySQL DATETIME 最高 6 位小数秒，需要截断
        return new DateTimePeriod(begin.atStartOfDay(), end.atTime(LocalTime.MAX.truncatedTo(ChronoUnit.MICROS)));
    }

    public List<LocalDate> toList() {
        List<LocalDate> list = new LinkedList<>();
        LocalDate current = this.getBegin();
        while (!current.isAfter(this.getEnd())) {
            list.add(current);
            current = current.plusDays(1);
        }
        return list;
    }

    /**
     * 是否为整月区间（含整年、跨多月）：begin 是所在月月初，end 是「自身所在月」月末。
     *
     * @return true 表示区间由若干个完整自然月组成
     */
    public boolean isWholeMonths() {
        return begin != null && end != null
                && begin.isEqual(begin.with(TemporalAdjusters.firstDayOfMonth()))
                && end.isEqual(end.with(TemporalAdjusters.lastDayOfMonth()));
    }

    public static DatePeriod getLastPeriod(DatePeriod datePeriod) {
        LocalDate begin = datePeriod.getBegin();
        LocalDate end = datePeriod.getEnd();

        // 整月区间：回退相同月数得到上一等长周期（如 6-7月→4-5月，5-7月→2-4月，整年→上一年）。
        if (datePeriod.isWholeMonths()) {
            int months = (int) ChronoUnit.MONTHS.between(YearMonth.from(begin), YearMonth.from(end)) + 1;
            return new DatePeriod(begin.minusMonths(months),
                    end.minusMonths(months).with(TemporalAdjusters.lastDayOfMonth())
            );
        }

        // 非整月区间：按天数平移，紧邻当前区间的前一段等长天数
        int period = (int) (end.toEpochDay() - begin.toEpochDay());
        LocalDate lastEnd = begin.minusDays(1);
        LocalDate lastBegin = lastEnd.minusDays(period);
        return new DatePeriod(lastBegin, lastEnd);
    }

    /**
     * 计算环比周期
     *
     * @return 环比周期
     */
    public DatePeriod getLastPeriod() {
        return getLastPeriod(this);
    }

    public static DatePeriod getMonthDays(DatePeriod statDatePeriod) {
        LocalDate start = statDatePeriod.getBegin().withDayOfMonth(1);
        LocalDate end = statDatePeriod.getEnd().with(TemporalAdjusters.lastDayOfMonth());
        return new DatePeriod(start, end);
    }
}
