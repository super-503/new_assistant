package com.zengame.ai.business.paper.domain.vo;

import lombok.Builder;
import lombok.Value;

/** 论文结果的公开题录及来源链接。 */
@Value
@Builder
public class PaperHitVO {

    String title;
    String authors;
    String publication;
    String publicationDate;
    String summary;
    String doi;
    String url;
}
