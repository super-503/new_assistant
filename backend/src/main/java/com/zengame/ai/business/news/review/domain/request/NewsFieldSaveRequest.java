package com.zengame.ai.business.news.review.domain.request;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 保存一条资讯上人工修改的 AI 字段。
 */
@Data
public class NewsFieldSaveRequest {

    /**
     * 修改后的字段值，只需包含改动的字段
     */
    @Valid
    @NotEmpty
    private List<NewsFieldValueRequest> fields;
}
