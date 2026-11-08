package com.zengame.ai.business.news.library.domain.vo;

import com.zengame.ai.business.news.review.domain.vo.NewsTagFieldVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 资讯库筛选栏的可选项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsLibraryFilterOptionsVO {

    /**
     * 启用中的标签字段：先是普通标签字段，再是岗位分类
     */
    private List<NewsTagFieldVO> tagFields;
}
