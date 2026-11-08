package com.zengame.ai.business.news.library.domain.bo;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import lombok.Data;

/**
 * 按类型、渠道、订阅源分组的资讯条数。
 */
@Data
public class NewsItemGroupCountBO {

    private NewsContentType contentType;

    private String channelCode;

    private Long feedId;

    private Long itemCount;
}
