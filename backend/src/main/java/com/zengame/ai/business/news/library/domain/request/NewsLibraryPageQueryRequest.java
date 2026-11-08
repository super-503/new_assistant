package com.zengame.ai.business.news.library.domain.request;

import com.zengame.ai.infrastructure.common.BasePageInfo;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 资讯库分页请求。查询条件和分页参数分开承载。
 */
@Data
public class NewsLibraryPageQueryRequest {

    @Valid
    @NotNull
    private NewsLibraryPageRequest query;

    private BasePageInfo basePageInfo = new BasePageInfo();
}
