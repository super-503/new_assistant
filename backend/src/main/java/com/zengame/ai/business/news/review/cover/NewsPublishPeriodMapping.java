package com.zengame.ai.business.news.review.cover;

import com.zengame.ai.infrastructure.common.DatePeriod;
import com.zengame.ai.infrastructure.common.DateTimePeriod;
import com.zengame.ai.infrastructure.exception.BusinessException;
import org.apache.commons.collections4.CollectionUtils;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 发布日期区间条件到发布时间上下界的转换，审核队列和资讯库共用。
 * 区间为 [起始日, 结束日]，两端都包含；不传表示不限。
 */
public class NewsPublishPeriodMapping {

    /**
     * @param period [起始日, 结束日]，可为空
     * @return 起始日零点；未传时为 null
     * @throws BusinessException 不是起止两个日期，或起始日晚于结束日
     */
    @Named("periodBegin")
    public LocalDateTime periodBegin(List<LocalDate> period) {
        return toDateTimePeriod(period).map(DateTimePeriod::getBegin).orElse(null);
    }

    /**
     * @param period [起始日, 结束日]，可为空
     * @return 结束日最后一刻；未传时为 null
     * @throws BusinessException 不是起止两个日期，或起始日晚于结束日
     */
    @Named("periodEnd")
    public LocalDateTime periodEnd(List<LocalDate> period) {
        return toDateTimePeriod(period).map(DateTimePeriod::getEnd).orElse(null);
    }

    private Optional<DateTimePeriod> toDateTimePeriod(List<LocalDate> period) {
        if (CollectionUtils.isEmpty(period)) {
            return Optional.empty();
        }
        DatePeriod datePeriod = DatePeriod.of(period);
        if (datePeriod.getBegin().isAfter(datePeriod.getEnd())) {
            throw new BusinessException("发布日期区间的起始日不能晚于结束日");
        }
        return Optional.of(datePeriod.toDateTimePeriod());
    }
}
