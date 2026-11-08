package com.zengame.ai.repository.ai.mapper;

import com.zengame.ai.repository.ai.po.NewsChannelRunPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资讯渠道运行记录持久化接口。运行记录在渠道运行结束时一次写入，不再修改。
 */
@Mapper
public interface NewsChannelRunMapper {

    /**
     * 写入一条运行记录，new_count 为空时取库表默认值，主键回填到 {@code record.id}。
     *
     * @param record 运行记录
     * @return 影响行数
     */
    int insert(NewsChannelRunPO record);

    /**
     * @param id 主键
     * @return 运行记录；不存在时为 null
     */
    NewsChannelRunPO selectById(@Param("id") Long id);

    /**
     * @param channelCode 渠道标识
     * @param limit       最多返回条数
     * @return 渠道最近的运行记录，按开始时间倒序
     */
    List<NewsChannelRunPO> selectRecentByChannelCode(@Param("channelCode") String channelCode,
                                                     @Param("limit") int limit);
}
