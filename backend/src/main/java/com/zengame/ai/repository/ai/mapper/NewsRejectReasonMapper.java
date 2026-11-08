package com.zengame.ai.repository.ai.mapper;

import com.zengame.ai.repository.ai.po.NewsRejectReasonPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资讯拒绝理由持久化接口。
 */
@Mapper
public interface NewsRejectReasonMapper {

    /**
     * 新增拒绝理由，为空的字段取库表默认值，主键回填到 {@code record.id}。
     *
     * @param record 拒绝理由
     * @return 影响行数
     */
    int insert(NewsRejectReasonPO record);

    /**
     * @param id 主键
     * @return 拒绝理由；不存在时为 null
     */
    NewsRejectReasonPO selectById(@Param("id") Long id);

    /**
     * 按主键整体覆盖可变字段：name、enabled、sort_no。调用方先查出整行再修改。
     *
     * @param record 拒绝理由
     * @return 影响行数
     */
    int updateById(NewsRejectReasonPO record);

    /**
     * @return 全部拒绝理由，按 sort_no、主键升序
     */
    List<NewsRejectReasonPO> selectAll();

    /**
     * @return 启用中的拒绝理由，按 sort_no、主键升序
     */
    List<NewsRejectReasonPO> selectEnabled();
}
