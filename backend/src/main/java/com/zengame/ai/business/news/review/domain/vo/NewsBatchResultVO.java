package com.zengame.ai.business.news.review.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量操作结果：逐条执行，各条互不影响。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsBatchResultVO {

    private int successCount;

    /**
     * 失败项，按请求顺序
     */
    private List<NewsBatchFailureVO> failures;
}
