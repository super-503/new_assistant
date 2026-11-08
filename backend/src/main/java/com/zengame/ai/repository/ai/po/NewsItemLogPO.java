package com.zengame.ai.repository.ai.po;

import com.zengame.ai.business.news.domain.enums.NewsItemAction;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资讯处理记录（news_item_log）：单条资讯的时间线，只追加不修改。
 */
@Data
public class NewsItemLogPO {

    private Long id;

    /**
     * 资讯
     */
    private Long itemId;

    private NewsItemAction action;

    /**
     * 操作人登录名；系统动作为空
     */
    private String operator;

    /**
     * 附加信息 JSON，如拒绝理由、修改的字段；没有时为空
     */
    private String detailJson;

    /**
     * 发生时间
     */
    private LocalDateTime createTime;
}
