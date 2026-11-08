package com.zengame.ai.business.news.setting;

import com.zengame.ai.apiclient.feishu.FeishuAppRobotClient;
import com.zengame.ai.apiclient.feishu.FeishuRobotClient;
import com.zengame.ai.business.news.setting.cover.NewsFeishuNotificationSettingConvert;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuNotificationConfig;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuAppRequest;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuNotificationSaveRequest;
import com.zengame.ai.business.news.setting.domain.vo.NewsFeishuNotificationSettingVO;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.NewsFeishuNotificationSettingMapper;
import com.zengame.ai.repository.ai.po.NewsFeishuNotificationSettingPO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 飞书通知的管理、测试和日报发送目标选择。 */
@Service
@RequiredArgsConstructor
public class NewsFeishuNotificationSettingService {

    private static final NewsFeishuNotificationSettingVO UNCONFIGURED =
            new NewsFeishuNotificationSettingVO("", "", false, false);

    private final NewsFeishuNotificationSettingMapper mapper;
    private final FeishuAppRobotClient appClient;
    private final FeishuRobotClient webhookClient;
    private final NewsProperties properties;

    /** @return 管理端可查看的飞书通知配置状态 */
    public NewsFeishuNotificationSettingVO get() {
        return Optional.ofNullable(mapper.select())
                .map(NewsFeishuNotificationSettingConvert.INSTANCE::toConfig)
                .map(NewsFeishuNotificationSettingConvert.INSTANCE::toVO)
                .orElse(UNCONFIGURED);
    }

    /**
     * 保存飞书通知的应用凭证、接收人和启停状态。
     * @param request 飞书通知配置表单
     * @return 已保存的配置状态
     * @throws BusinessException 新应用缺少 App Secret
     */
    @Transactional(rollbackFor = Exception.class)
    public NewsFeishuNotificationSettingVO save(NewsFeishuNotificationSaveRequest request) {
        NewsFeishuNotificationConfig config = resolve(request, mapper::selectForUpdate);
        mapper.upsert(NewsFeishuNotificationSettingConvert.INSTANCE.toPO(config));
        return NewsFeishuNotificationSettingConvert.INSTANCE.toVO(config);
    }

    /**
     * 向表单指定的飞书接收人发送一条测试消息。
     * @param request 待测试的飞书配置
     * @throws BusinessException 缺少 App Secret，或飞书未确认接收
     */
    public void verify(NewsFeishuNotificationSaveRequest request) {
        appSender(resolve(request, mapper::select)).accept("资讯日报 · 测试消息\n飞书应用机器人单聊发送成功。");
    }

    /**
     * 获取飞书接收人授权使用的应用凭证。
     * @param request 通知表单中的应用凭证
     * @return 同一个飞书应用的完整凭证
     * @throws BusinessException 未提供或保存该应用的 App Secret
     */
    public FeishuAppRobotClient.AppCredentials authorizationCredentials(NewsFeishuAppRequest request) {
        return new FeishuAppRobotClient.AppCredentials(request.getAppId(), resolveSecret(request, mapper::select));
    }

    /**
     * 获取本次日报的发送目标。已保存的应用配置决定启停，未设置应用时使用部署配置中的群机器人。
     * @return 本次日报使用的发送器；没有启用的通知目标时为空
     */
    public Optional<Consumer<String>> digestSender() {
        Optional<NewsFeishuNotificationSettingPO> setting = Optional.ofNullable(mapper.select());
        if (setting.isPresent()) {
            return setting.filter(NewsFeishuNotificationSettingPO::isEnabled)
                    .map(NewsFeishuNotificationSettingConvert.INSTANCE::toConfig)
                    .map(this::appSender);
        }
        return Optional.ofNullable(properties.getDigestFeishuWebhook()).filter(StringUtils::isNotBlank)
                .map(webhook -> message -> webhookClient.sendRobotMessage(webhook, message));
    }

    /**
     * @param request 飞书通知表单
     * @param savedSetting 同一个应用可沿用的凭证来源
     * @return 带有效密钥的表单配置
     * @throws BusinessException 表单及已保存的同应用配置都缺少密钥
     */
    private NewsFeishuNotificationConfig resolve(NewsFeishuNotificationSaveRequest request,
                                                 Supplier<NewsFeishuNotificationSettingPO> savedSetting) {
        return NewsFeishuNotificationSettingConvert.INSTANCE.toConfig(request, resolveSecret(request, savedSetting));
    }

    /** @return 表单提供的密钥或已保存的同应用密钥 */
    private String resolveSecret(NewsFeishuAppRequest request, Supplier<NewsFeishuNotificationSettingPO> savedSetting) {
        return Optional.ofNullable(StringUtils.trimToNull(request.getAppSecret()))
                .or(() -> Optional.ofNullable(savedSetting.get())
                        .filter(setting -> setting.getAppId().equals(request.getAppId()))
                        .map(NewsFeishuNotificationSettingPO::getAppSecret))
                .orElseThrow(() -> new BusinessException("请填写该飞书应用的 App Secret"));
    }

    /** @return 指向该应用单聊接收人的发送器 */
    private Consumer<String> appSender(NewsFeishuNotificationConfig config) {
        return appClient.sender(NewsFeishuNotificationSettingConvert.INSTANCE.toRecipient(config));
    }
}
