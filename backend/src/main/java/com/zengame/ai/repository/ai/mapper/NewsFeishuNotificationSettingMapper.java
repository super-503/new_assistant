package com.zengame.ai.repository.ai.mapper;

import com.zengame.ai.repository.ai.po.NewsFeishuChannelConfigPO;
import com.zengame.ai.repository.ai.po.NewsFeishuAuthorizedUserPO;
import com.zengame.ai.repository.ai.po.NewsFeishuNotificationSettingPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** 飞书应用机器人通知配置的持久化接口。 */
@Mapper
public interface NewsFeishuNotificationSettingMapper {

    NewsFeishuNotificationSettingPO select();

    NewsFeishuNotificationSettingPO selectForUpdate();

    int upsert(NewsFeishuNotificationSettingPO setting);

    NewsFeishuChannelConfigPO selectChannel();

    NewsFeishuChannelConfigPO selectChannelForUpdate();

    int updateChannel(@Param("configJson") String configJson);

    List<NewsFeishuAuthorizedUserPO> selectAuthorizedUsers(@Param("appId") String appId);

    NewsFeishuAuthorizedUserPO selectAuthorizedUser(@Param("appId") String appId, @Param("openId") String openId);

    int upsertAuthorizedUser(NewsFeishuAuthorizedUserPO user);
}
