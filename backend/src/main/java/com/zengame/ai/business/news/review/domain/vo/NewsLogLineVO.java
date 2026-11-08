package com.zengame.ai.business.news.review.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 处理记录的一行附加信息，如「拒绝理由：营销推广」。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsLogLineVO {

    /**
     * 名称，如拒绝理由、补充说明、修改字段、未生成字段、失败原因
     */
    private String label;

    private String value;

    /**
     * 是否以错误色强调
     */
    private boolean danger;
}
