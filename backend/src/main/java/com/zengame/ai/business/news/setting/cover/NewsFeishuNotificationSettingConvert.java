package com.zengame.ai.business.news.setting.cover;

import com.zengame.ai.apiclient.feishu.FeishuAppRobotClient;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuNotificationConfig;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuNotificationSaveRequest;
import com.zengame.ai.business.news.setting.domain.vo.NewsFeishuAuthorizedUserVO;
import com.zengame.ai.business.news.setting.domain.vo.NewsFeishuNotificationSettingVO;
import com.zengame.ai.repository.ai.po.NewsFeishuNotificationSettingPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/** 飞书通知的表单、发送配置、存储记录和管理端状态之间的转换。 */
@Mapper
public interface NewsFeishuNotificationSettingConvert {

    NewsFeishuNotificationSettingConvert INSTANCE = Mappers.getMapper(NewsFeishuNotificationSettingConvert.class);

    /** 从持久化记录读取可用于发送的配置。 */
    NewsFeishuNotificationConfig toConfig(NewsFeishuNotificationSettingPO setting);

    /** 从表单和本次使用的 App Secret 生成发送配置。 */
    @Mapping(target = "appSecret", source = "secret")
    NewsFeishuNotificationConfig toConfig(NewsFeishuNotificationSaveRequest request, String secret);

    /** 将飞书通知配置转换为持久化记录。 */
    @Mapping(target = "id", ignore = true)
    NewsFeishuNotificationSettingPO toPO(NewsFeishuNotificationConfig config);

    /** 返回已保存的发送状态和接收人。 */
    @Mapping(target = "configured", constant = "true")
    NewsFeishuNotificationSettingVO toVO(NewsFeishuNotificationConfig config);

    /** 返回飞书单聊所需的应用凭证和接收人。 */
    FeishuAppRobotClient.Recipient toRecipient(NewsFeishuNotificationConfig config);

    /** 返回通知配置页可展示的飞书授权人。 */
    NewsFeishuAuthorizedUserVO toAuthorizedUser(FeishuAppRobotClient.AuthorizedUser user);
}
