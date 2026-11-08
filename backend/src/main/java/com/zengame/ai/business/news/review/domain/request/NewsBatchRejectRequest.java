package com.zengame.ai.business.news.review.domain.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 以同一个理由批量拒绝资讯。
 */
@Data
public class NewsBatchRejectRequest {

    @NotEmpty
    private List<Long> ids;

    /**
     * 启用中的拒绝理由
     */
    @NotNull
    private Long reasonId;

    /**
     * 补充说明，选填
     */
    @Size(max = 500)
    private String note;
}
