package com.zengame.ai.business.news.review.cover;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsItemStatus;
import com.zengame.ai.business.news.processing.StringListJson;
import com.zengame.ai.business.news.review.domain.bo.NewsFieldEditBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemBO;
import com.zengame.ai.business.news.review.domain.bo.NewsItemQueryBO;
import com.zengame.ai.business.news.review.domain.enums.NewsReviewSort;
import com.zengame.ai.business.news.review.domain.request.NewsFieldValueRequest;
import com.zengame.ai.business.news.review.domain.request.NewsReviewPageRequest;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.po.NewsItemPO;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;

/**
 * 资讯 PO 与审核用 BO 的互转，以及审核队列查询条件、字段修改请求到 BO 的转换。
 */
@Mapper(uses = NewsPublishPeriodMapping.class)
public interface NewsItemConvert {

    NewsItemConvert INSTANCE = Mappers.getMapper(NewsItemConvert.class);

    @Mapping(target = "aiValues", source = "aiValuesJson", qualifiedByName = "readObject")
    @Mapping(target = "aiFailedKeys", source = "aiFailedKeysJson", qualifiedByName = "readKeys")
    @Mapping(target = "editedKeys", source = "editedKeysJson", qualifiedByName = "readKeys")
    NewsItemBO toBO(NewsItemPO po);

    @Mapping(target = "aiValuesJson", source = "aiValues", qualifiedByName = "writeObject")
    @Mapping(target = "aiFailedKeysJson", source = "aiFailedKeys", qualifiedByName = "writeKeys")
    @Mapping(target = "editedKeysJson", source = "editedKeys", qualifiedByName = "writeKeys")
    @Mapping(target = "digestSentTime", ignore = true)
    NewsItemPO toPO(NewsItemBO bo);

    @Mapping(target = "status", source = "status", qualifiedByName = "itemStatus")
    @Mapping(target = "contentType", source = "contentType", qualifiedByName = "contentType")
    @Mapping(target = "channelCode", source = "channelCode", qualifiedByName = "trimToNull")
    @Mapping(target = "keyword", source = "keyword", qualifiedByName = "trimToNull")
    @Mapping(target = "oldestFirst", source = "sort", qualifiedByName = "oldestFirst")
    @Mapping(target = "publishBegin", source = "publishPeriod", qualifiedByName = "periodBegin")
    @Mapping(target = "publishEnd", source = "publishPeriod", qualifiedByName = "periodEnd")
    NewsItemQueryBO toQuery(NewsReviewPageRequest request);

    NewsFieldEditBO toEdit(NewsFieldValueRequest request);

    List<NewsFieldEditBO> toEdits(List<NewsFieldValueRequest> requests);

    /**
     * @param json JSON 对象
     * @return 解析后的对象节点
     */
    @Named("readObject")
    default ObjectNode readObject(String json) {
        return (ObjectNode) JacksonUtil.readTree(json);
    }

    /**
     * @param node 对象节点
     * @return JSON 对象
     */
    @Named("writeObject")
    default String writeObject(ObjectNode node) {
        return node.toString();
    }

    /**
     * @param json JSON 字符串数组
     * @return 字段标识
     */
    @Named("readKeys")
    default List<String> readKeys(String json) {
        return StringListJson.read(json);
    }

    /**
     * @param keys 字段标识
     * @return JSON 字符串数组
     */
    @Named("writeKeys")
    default String writeKeys(List<String> keys) {
        return StringListJson.write(keys);
    }

    /**
     * @param code 资讯状态编码
     * @return 资讯状态
     */
    @Named("itemStatus")
    default NewsItemStatus itemStatus(String code) {
        return CodeEnumUtils.fromCode(code, NewsItemStatus.class);
    }

    /**
     * @param code 内容类型编码，可为空
     * @return 内容类型；未传时为 null，表示不限
     */
    @Named("contentType")
    default NewsContentType contentType(String code) {
        return Optional.ofNullable(code)
                .filter(StringUtils::isNotBlank)
                .map(value -> CodeEnumUtils.fromCode(value, NewsContentType.class))
                .orElse(null);
    }

    /**
     * @param sort 排序编码
     * @return 按发布时间从旧到新时为 true
     */
    @Named("oldestFirst")
    default boolean oldestFirst(String sort) {
        return CodeEnumUtils.fromCode(sort, NewsReviewSort.class) == NewsReviewSort.OLDEST;
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
