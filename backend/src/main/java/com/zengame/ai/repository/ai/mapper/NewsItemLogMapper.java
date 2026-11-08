package com.zengame.ai.repository.ai.mapper;

import com.zengame.ai.repository.ai.po.NewsItemLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资讯处理记录持久化接口。处理记录只追加不修改。
 */
@Mapper
public interface NewsItemLogMapper {

    /**
     * 追加一条处理记录，create_time 为空时取当前时间，主键回填到 {@code record.id}。
     *
     * @param record 处理记录
     * @return 影响行数
     */
    int insert(NewsItemLogPO record);

    /**
     * @param id 主键
     * @return 处理记录；不存在时为 null
     */
    NewsItemLogPO selectById(@Param("id") Long id);

    /**
     * @param itemId 资讯主键
     * @return 该资讯的完整时间线，按发生先后升序
     */
    List<NewsItemLogPO> selectByItemId(@Param("itemId") Long itemId);
}
