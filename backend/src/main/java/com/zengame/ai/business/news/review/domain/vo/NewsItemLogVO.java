package com.zengame.ai.business.news.review.domain.vo;

import com.zengame.ai.business.news.domain.enums.NewsItemAction;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资讯处理记录的一条。
 */
@Data
public class NewsItemLogVO {

    private Long id;

    /**
     * 发生时间
     */
    private LocalDateTime time;

    private NewsItemAction action;

    /**
     * 动作中文名，如审核通过、恢复到待审
     */
    private String actionName;

    /**
     * 操作人登录名；系统动作为「系统」
     */
    private String operator;

    /**
     * 附加信息，没有时为空数组
     */
    private List<NewsLogLineVO> lines;
}
