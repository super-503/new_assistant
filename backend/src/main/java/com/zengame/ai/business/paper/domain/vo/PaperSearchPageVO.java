package com.zengame.ai.business.paper.domain.vo;

import lombok.Value;

import java.util.List;

/** 单个来源的论文结果和可继续检索时的游标。 */
@Value
public class PaperSearchPageVO {

    List<PaperHitVO> items;
    String nextCursor;
}
