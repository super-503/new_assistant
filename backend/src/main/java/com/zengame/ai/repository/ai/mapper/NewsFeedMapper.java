package com.zengame.ai.repository.ai.mapper;

import com.zengame.ai.business.news.domain.enums.NewsContentType;
import com.zengame.ai.business.news.domain.enums.NewsFeedPullStatus;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资讯订阅源持久化接口。
 */
@Mapper
public interface NewsFeedMapper {

    /**
     * 新增订阅源，为空的字段取库表默认值，主键回填到 {@code record.id}。
     *
     * @param record 订阅源
     * @return 影响行数
     */
    int insert(NewsFeedPO record);

    /**
     * @param id 主键
     * @return 订阅源；不存在时为 null
     */
    NewsFeedPO selectById(@Param("id") Long id);

    /**
     * 按主键整体覆盖可变字段：source_name、display_name、avatar_url、description、content_type、enabled、
     * pull_status、pull_error、last_pull_time、cursor_time。渠道和来源标识不可修改。调用方先查出整行再修改。
     *
     * @param record 订阅源
     * @return 影响行数
     */
    int updateById(NewsFeedPO record);

    /**
     * @param channelCode 渠道标识
     * @return 渠道下的全部订阅源，按主键升序
     */
    List<NewsFeedPO> selectByChannelCode(@Param("channelCode") String channelCode);

    /**
     * @param channelCode 渠道标识
     * @return 渠道下启用中的订阅源，按主键升序
     */
    List<NewsFeedPO> selectEnabledByChannelCode(@Param("channelCode") String channelCode);

    /**
     * @param channelCode 渠道标识
     * @param sourceKey   插件内的来源标识
     * @return 订阅源；不存在时为 null
     */
    NewsFeedPO selectByChannelAndSourceKey(@Param("channelCode") String channelCode,
                                           @Param("sourceKey") String sourceKey);

    /**
     * 修改订阅源的运营设置：显示名称、内容类型、是否启用；不影响拉取状态和进度。
     *
     * @param id          主键
     * @param displayName 显示名称
     * @param contentType 内容类型
     * @param enabled     是否启用
     * @return 影响行数
     */
    int updateSettingsById(@Param("id") Long id,
                           @Param("displayName") String displayName,
                           @Param("contentType") NewsContentType contentType,
                           @Param("enabled") Boolean enabled);

    /**
     * @param id      主键
     * @param enabled 是否启用
     * @return 影响行数
     */
    int updateEnabledById(@Param("id") Long id, @Param("enabled") Boolean enabled);

    /**
     * 物理删除订阅源，已入库的资讯保留。
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 写入一次拉取的结果：拉取状态、失败原因、最近拉取时间和拉取进度；不影响运营设置。
     *
     * @param id           主键
     * @param pullStatus   拉取状态
     * @param pullError    失败原因；成功时为空
     * @param lastPullTime 最近一次成功拉取的时间
     * @param cursorTime   拉取进度
     * @return 影响行数
     */
    int updatePullStateById(@Param("id") Long id,
                            @Param("pullStatus") NewsFeedPullStatus pullStatus,
                            @Param("pullError") String pullError,
                            @Param("lastPullTime") LocalDateTime lastPullTime,
                            @Param("cursorTime") LocalDateTime cursorTime);
}
