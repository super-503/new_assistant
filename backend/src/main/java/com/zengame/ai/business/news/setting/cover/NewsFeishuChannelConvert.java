package com.zengame.ai.business.news.setting.cover;

import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuChannelConfig;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuChannelSaveRequest;
import com.zengame.ai.business.news.setting.domain.vo.NewsFeishuChannelVO;
import com.zengame.ai.repository.ai.po.NewsFeishuNotificationSettingPO;
import com.zengame.ai.repository.ai.po.NewsFeishuAuthorizedUserPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/** 飞书渠道配置与管理端表单、展示模型之间的转换。 */
@Mapper
public interface NewsFeishuChannelConvert {
    NewsFeishuChannelConvert INSTANCE = Mappers.getMapper(NewsFeishuChannelConvert.class);

    /** @return 管理页面可见的渠道配置 */
    NewsFeishuChannelVO toVO(NewsFeishuChannelConfig config, long revision);

    /** @return 不含密钥的应用展示信息 */
    NewsFeishuChannelVO.App toVO(NewsFeishuChannelConfig.App app);

    /** @return 推送人员展示信息 */
    NewsFeishuChannelVO.Recipient toVO(NewsFeishuChannelConfig.Recipient recipient);

    /** @return 带本次有效密钥的应用配置 */
    @Mapping(target = "appSecret", source = "secret")
    NewsFeishuChannelConfig.App toApp(NewsFeishuChannelSaveRequest.App app, String secret);

    /** @return 已授权的接收人及所属应用 */
    NewsFeishuChannelConfig.Recipient toRecipient(NewsFeishuAuthorizedUserPO user);

    /** @return 现有单应用的凭证配置 */
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    NewsFeishuChannelConfig.App toLegacyApp(NewsFeishuNotificationSettingPO setting);

    /** @return 现有单应用的接收人配置 */
    @Mapping(target = "name", source = "recipientName")
    @Mapping(target = "avatarUrl", source = "recipientAvatarUrl")
    NewsFeishuChannelConfig.Recipient toLegacyRecipient(NewsFeishuNotificationSettingPO setting);
}
