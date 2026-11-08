package com.zengame.ai.business.news.review.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 处理记录的附加信息，以 JSON 存于 news_item_log.detail_json。
 * <ul>
 *     <li>REJECTED：拒绝理由和补充说明；</li>
 *     <li>FIELD_EDITED：修改的字段；</li>
 *     <li>PROCESSED / PROCESS_FAILED：参与生成的字段、未生成的字段和整次调用失败的原因，由数据加工写入。</li>
 * </ul>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsItemLogDetailBO {

    private Long reasonId;

    /**
     * 拒绝时的理由名称
     */
    private String reasonName;

    /**
     * 拒绝补充说明
     */
    private String note;

    /**
     * 修改的字段标识；加工记录里为参与生成的字段标识
     */
    private List<String> fieldKeys;

    /**
     * 加工记录里未生成的字段标识
     */
    private List<String> failedKeys;

    /**
     * 加工记录里整次调用失败的原因
     */
    private String error;
}
