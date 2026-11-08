package com.zengame.ai.business.news.setting.domain.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * AI 字段排序：全部字段的 id，按新的展示顺序。
 */
@Data
public class NewsAiFieldSortRequest {

    @NotEmpty(message = "字段顺序不能为空")
    private List<Long> ids;
}
