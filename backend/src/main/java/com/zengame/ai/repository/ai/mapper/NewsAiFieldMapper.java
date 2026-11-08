package com.zengame.ai.repository.ai.mapper;

import com.zengame.ai.repository.ai.po.NewsAiFieldPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资讯 AI 字段持久化接口。
 */
@Mapper
public interface NewsAiFieldMapper {

    /**
     * 新增 AI 字段，为空的字段取库表默认值，主键回填到 {@code record.id}。
     *
     * @param record AI 字段
     * @return 影响行数
     */
    int insert(NewsAiFieldPO record);

    /**
     * @param id 主键
     * @return AI 字段；不存在时为 null
     */
    NewsAiFieldPO selectById(@Param("id") Long id);

    /**
     * 按主键整体覆盖可变字段：field_key、name、source、output_type、multiline、options_json、show_as_tag、prompt、
     * required、editable、enabled、article_only、sort_no。种类不可修改。调用方先查出整行再修改。
     *
     * @param record AI 字段
     * @return 影响行数
     */
    int updateById(NewsAiFieldPO record);

    int updateSortNo(@Param("id") Long id, @Param("sortNo") int sortNo);

    /**
     * @param fieldKey 字段标识
     * @return AI 字段；不存在时为 null
     */
    NewsAiFieldPO selectByFieldKey(@Param("fieldKey") String fieldKey);

    /**
     * @return 全部 AI 字段，按 sort_no、主键升序
     */
    List<NewsAiFieldPO> selectAll();

    /**
     * @return 启用中的 AI 字段，按 sort_no、主键升序，即详情页的展示顺序
     */
    List<NewsAiFieldPO> selectEnabled();

    /**
     * 删除 AI 字段，已有资讯里该字段的结果保留。
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}
