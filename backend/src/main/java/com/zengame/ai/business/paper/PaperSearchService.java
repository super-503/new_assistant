package com.zengame.ai.business.paper;

import com.zengame.ai.apiclient.paper.ArxivClient;
import com.zengame.ai.apiclient.paper.CnkiClient;
import com.zengame.ai.apiclient.paper.CrossrefClient;
import com.zengame.ai.apiclient.paper.EuropePmcClient;
import com.zengame.ai.business.paper.domain.enums.PaperSource;
import com.zengame.ai.business.paper.domain.request.PaperSearchRequest;
import com.zengame.ai.business.paper.domain.vo.PaperSearchPageVO;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.CodeEnumUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 选择论文目录并检索一页题录。 */
@Service
@RequiredArgsConstructor
public class PaperSearchService {

    private final CnkiClient cnkiClient;
    private final ArxivClient arxivClient;
    private final CrossrefClient crossrefClient;
    private final EuropePmcClient europePmcClient;

    /**
     * @param request 关键词、来源标识和该来源的分页游标
     * @return 论文题录和下一页游标
     * @throws BusinessException 来源或游标无效，或来源暂时不可用
     */
    public PaperSearchPageVO search(PaperSearchRequest request) {
        PaperSource source = CodeEnumUtils.fromCode(request.getSource(), PaperSource.class);
        String keyword = request.getKeyword().strip();
        String cursor = request.getCursor();
        switch (source) {
            case CNKI:
                return cnkiClient.search(keyword, cursor);
            case ARXIV:
                return arxivClient.search(keyword, cursor);
            case CROSSREF:
                return crossrefClient.search(keyword, cursor);
            case EUROPE_PMC:
                return europePmcClient.search(keyword, cursor);
            default:
                throw new IllegalStateException("论文来源尚未配置: " + source);
        }
    }
}
