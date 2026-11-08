package com.zengame.ai.business.news.library.cover;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.library.domain.request.NewsLibraryPageRequest;
import com.zengame.ai.business.news.review.cover.NewsPublishPeriodMapping;
import com.zengame.ai.business.news.review.domain.bo.NewsItemQueryBO;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * 资讯库查询条件到资讯查询 BO 的转换：只查已通过的资讯，按发布时间从新到旧。
 */
@Mapper(uses = NewsPublishPeriodMapping.class)
public interface NewsLibraryConvert {

    NewsLibraryConvert INSTANCE = Mappers.getMapper(NewsLibraryConvert.class);

    @Mapping(target = "status", constant = "APPROVED")
    @Mapping(target = "contentType", source = "contentType", qualifiedByName = "contentType")
    @Mapping(target = "channelCode", source = "channelCode", qualifiedByName = "trimToNull")
    @Mapping(target = "keyword", source = "keyword", qualifiedByName = "trimToNull")
    @Mapping(target = "publishBegin", source = "publishPeriod", qualifiedByName = "periodBegin")
    @Mapping(target = "publishEnd", source = "publishPeriod", qualifiedByName = "periodEnd")
    @Mapping(target = "oldestFirst", constant = "false")
    NewsItemQueryBO toQuery(NewsLibraryPageRequest request);

    /**
     * @param code 内容类型编码
     * @return 内容类型
     */
    @Named("contentType")
    default NewsContentType contentType(String code) {
        return CodeEnumUtils.fromCode(code, NewsContentType.class);
    }

    /**
     * @param value 文本条件
     * @return 去掉首尾空白；空白时为 null，表示不限
     */
    @Named("trimToNull")
    default String trimToNull(String value) {
        return StringUtils.trimToNull(value);
    }
}
