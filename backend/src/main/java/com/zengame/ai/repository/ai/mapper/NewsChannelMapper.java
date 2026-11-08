package com.zengame.ai.repository.ai.mapper;

import com.zengame.ai.business.news.domain.enums.NewsCredentialStatus;
import com.zengame.ai.repository.ai.po.NewsChannelPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资讯渠道持久化接口。
 */
@Mapper
public interface NewsChannelMapper {

    /**
     * 新增渠道，为空的字段取库表默认值，主键回填到 {@code record.id}。
     *
     * @param record 渠道
     * @return 影响行数
     */
    int insert(NewsChannelPO record);

    /**
     * @param id 主键
     * @return 渠道；不存在时为 null
     */
    NewsChannelPO selectById(@Param("id") Long id);

    /**
     * 按主键整体覆盖可变字段：name、enabled、credential_json、credential_status、credential_account、
     * credential_update_time。调用方先查出整行再修改。
     *
     * @param record 渠道
     * @return 影响行数
     */
    int updateById(NewsChannelPO record);

    /**
     * @param code 渠道标识
     * @return 渠道；不存在时为 null
     */
    NewsChannelPO selectByCode(@Param("code") String code);

    /**
     * @return 全部渠道，按主键升序
     */
    List<NewsChannelPO> selectAll();

    /**
     * 凭证状态为 {@code expected} 时改为 {@code target}，用于并发下只有一方完成状态切换。
     *
     * @param code     渠道标识
     * @param expected 切换前应处的凭证状态
     * @param target   切换后的凭证状态
     * @return 影响行数，0 表示渠道当前不处于 {@code expected}
     */
    int updateCredentialStatusByCode(@Param("code") String code,
                                     @Param("expected") NewsCredentialStatus expected,
                                     @Param("target") NewsCredentialStatus target);

    /**
     * 写入渠道凭证：credential_json、credential_status、credential_update_time；
     * credential_account 传 null 时保持原值。
     *
     * @param code                 渠道标识
     * @param credentialJson       凭证 JSON
     * @param credentialStatus     凭证状态
     * @param credentialAccount    登录账号；为 null 时不修改
     * @param credentialUpdateTime 凭证保存时间
     * @return 影响行数
     */
    int updateCredentialByCode(@Param("code") String code,
                               @Param("credentialJson") String credentialJson,
                               @Param("credentialStatus") NewsCredentialStatus credentialStatus,
                               @Param("credentialAccount") String credentialAccount,
                               @Param("credentialUpdateTime") LocalDateTime credentialUpdateTime);

    /**
     * @param code    渠道标识
     * @param enabled 是否启用
     * @return 影响行数
     */
    int updateEnabledByCode(@Param("code") String code, @Param("enabled") Boolean enabled);
}
