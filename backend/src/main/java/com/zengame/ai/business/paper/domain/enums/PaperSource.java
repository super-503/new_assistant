package com.zengame.ai.business.paper.domain.enums;

import com.zengame.ai.infrastructure.enums.CodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 论文检索可选来源。 */
@Getter
@AllArgsConstructor
public enum PaperSource implements CodeEnum {

    CNKI("中国知网"),
    ARXIV("arXiv"),
    CROSSREF("Crossref"),
    EUROPE_PMC("Europe PMC");

    private final String displayName;

    @Override
    public String getCode() {
        return name();
    }
}
