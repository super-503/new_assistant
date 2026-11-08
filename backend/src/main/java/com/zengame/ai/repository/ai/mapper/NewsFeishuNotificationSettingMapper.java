package com.zengame.ai.repository.ai.mapper;

import com.zengame.ai.repository.ai.po.NewsFeishuNotificationSettingPO;
import org.apache.ibatis.annotations.Mapper;

/** 飞书应用机器人通知配置的持久化接口。 */
@Mapper
public interface NewsFeishuNotificationSettingMapper {

    NewsFeishuNotificationSettingPO select();

    NewsFeishuNotificationSettingPO selectForUpdate();

    int upsert(NewsFeishuNotificationSettingPO setting);
}
