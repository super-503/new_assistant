package com.zengame.ai.business.news.setting;

import com.google.common.base.Suppliers;
import com.zengame.ai.apiclient.feishu.FeishuAppRobotClient;
import com.zengame.ai.apiclient.feishu.FeishuDigestCard;
import com.zengame.ai.apiclient.feishu.FeishuRobotClient;
import com.zengame.ai.business.news.library.NewsFeishuDigestService;
import com.zengame.ai.business.news.setting.cover.NewsFeishuNotificationSettingConvert;
import com.zengame.ai.business.news.setting.cover.NewsFeishuChannelConvert;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuChannelConfig;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuDigestDelivery;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuCardSettings;
import com.zengame.ai.business.news.setting.domain.bo.NewsDigestContentSettings;
import com.zengame.ai.business.news.setting.domain.bo.NewsPushPlanDefinition;
import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuNotificationConfig;
import com.zengame.ai.business.news.setting.domain.enums.NewsDigestDeliveryChannel;
import com.zengame.ai.business.news.setting.domain.enums.NewsPushPlanCard;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuAppRequest;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuCardTestRequest;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuChannelSaveRequest;
import com.zengame.ai.business.news.setting.domain.request.NewsFeishuNotificationSaveRequest;
import com.zengame.ai.business.news.setting.domain.request.NewsPushPlanPreviewRequest;
import com.zengame.ai.business.news.setting.domain.vo.NewsFeishuBotProfileVO;
import com.zengame.ai.business.news.setting.domain.vo.NewsFeishuAuthorizedUserVO;
import com.zengame.ai.business.news.setting.domain.vo.NewsFeishuChannelVO;
import com.zengame.ai.business.news.setting.domain.vo.NewsFeishuNotificationSettingVO;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.mapper.NewsFeishuNotificationSettingMapper;
import com.zengame.ai.repository.ai.po.NewsFeishuNotificationSettingPO;
import com.zengame.ai.repository.ai.po.NewsFeishuChannelConfigPO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** 飞书通知的管理、测试和日报发送目标选择。 */
@Service
@RequiredArgsConstructor
public class NewsFeishuNotificationSettingService {

    private static final NewsFeishuNotificationSettingVO UNCONFIGURED =
            new NewsFeishuNotificationSettingVO("", "", null, null, false, false);

    private final NewsFeishuNotificationSettingMapper mapper;
    private final FeishuAppRobotClient appClient;
    private final FeishuRobotClient webhookClient;
    private final NewsProperties properties;
    private final NewsFeishuDigestService digestService;

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
        return new FeishuAppRobotClient.AppCredentials(request.getAppId(),
                Optional.ofNullable(StringUtils.trimToNull(request.getAppSecret()))
                        .or(() -> savedChannelConfig(mapper.selectChannel())
                                .map(config -> Optional.ofNullable(findAppSecret(config, request.getAppId())))
                                .orElseGet(() -> resolveLegacySecret(request)))
                        .orElseThrow(() -> new BusinessException("请填写该飞书应用的 App Secret")));
    }

    /** @return 当前飞书应用、推送人员和日报卡片配置 */
    public NewsFeishuChannelVO getChannel() {
        NewsFeishuChannelConfigPO row = mapper.selectChannel();
        return NewsFeishuChannelConvert.INSTANCE.toVO(channelConfig(row), revision(row));
    }

    /**
     * @param appId 飞书应用 ID
     * @return 该应用下可以添加为推送接收人的授权人员
     */
    public List<NewsFeishuAuthorizedUserVO> listAuthorizedUsers(String appId) {
        if (!appId.matches("cli_[a-zA-Z0-9_-]+") || appId.length() > 128) {
            throw new BusinessException("请选择有效的飞书应用");
        }
        return mapper.selectAuthorizedUsers(appId).stream()
                .map(NewsFeishuNotificationSettingConvert.INSTANCE::toAuthorizedUser)
                .collect(Collectors.toList());
    }

    /**
     * @param appId 完成本次授权的飞书应用 ID
     * @param user 飞书返回的人员资料
     */
    public void rememberAuthorizedUser(String appId, NewsFeishuAuthorizedUserVO user) {
        mapper.upsertAuthorizedUser(NewsFeishuNotificationSettingConvert.INSTANCE.toAuthorizedUserPO(appId, user));
    }

    /**
     * @param request 编辑后的渠道配置及其版本
     * @return 保存后的渠道配置
     * @throws BusinessException 版本冲突、应用或人员重复、应用凭证缺失
     */
    @Transactional(rollbackFor = Exception.class)
    public NewsFeishuChannelVO saveChannel(NewsFeishuChannelSaveRequest request) {
        NewsFeishuChannelConfigPO row = mapper.selectChannelForUpdate();
        if (row == null) throw new BusinessException("请初始化飞书渠道配置表");
        if (revision(row) != request.getRevision()) {
            throw new BusinessException("飞书配置已被其他操作更新，请刷新页面后重试");
        }
        NewsFeishuChannelConfig current = channelConfig(row);
        Optional.ofNullable(request.getCard().getTemplate())
                .ifPresent(template -> FeishuDigestCard.parseVariables(template.getVariablesJson()));
        NewsFeishuChannelConfig result = new NewsFeishuChannelConfig();
        var appIds = new HashSet<String>();
        for (NewsFeishuChannelSaveRequest.App app : request.getApps()) {
            if (!appIds.add(app.getAppId())) throw new BusinessException("同一个飞书应用只能添加一次");
            String secret = Optional.ofNullable(StringUtils.trimToNull(app.getAppSecret()))
                    .orElseGet(() -> findAppSecret(current, app.getAppId()));
            if (StringUtils.isBlank(secret)) throw new BusinessException("请填写新增应用的 App Secret");
            result.getApps().add(NewsFeishuChannelConvert.INSTANCE.toApp(app, secret));
        }
        var recipientKeys = new HashSet<List<String>>();
        for (NewsFeishuChannelSaveRequest.Recipient recipient : request.getRecipients()) {
            if (!appIds.contains(recipient.getAppId())) throw new BusinessException("请选择有效的接收人所属应用");
            if (!recipientKeys.add(List.of(recipient.getAppId(), recipient.getOpenId()))) {
                throw new BusinessException("同一应用下的接收人只能添加一次");
            }
            result.getRecipients().add(resolveRecipient(current, recipient));
        }
        if (request.isEnabled() && result.getRecipients().isEmpty()) {
            throw new BusinessException("添加推送人员后才能启用日报");
        }
        result.setEnabled(request.isEnabled());
        result.setCard(request.getCard());
        result.setContent(Optional.ofNullable(request.getContent()).orElse(current.getContent()));
        mapper.updateChannel(JacksonUtil.writeValueAsString(result));
        return NewsFeishuChannelConvert.INSTANCE.toVO(result, revision(row) + 1);
    }

    /** @return 同一应用下已授权的接收人，或配置中已保存的接收人 */
    private NewsFeishuChannelConfig.Recipient resolveRecipient(NewsFeishuChannelConfig current,
                                                                NewsFeishuChannelSaveRequest.Recipient recipient) {
        String appId = recipient.getAppId();
        String openId = recipient.getOpenId();
        return Optional.ofNullable(mapper.selectAuthorizedUser(appId, openId))
                .map(NewsFeishuChannelConvert.INSTANCE::toRecipient)
                .or(() -> current.getRecipients().stream().filter(saved -> appId.equals(saved.getAppId())
                        && openId.equals(saved.getOpenId())).findFirst())
                .orElseThrow(() -> new BusinessException("请从所属飞书应用的已授权人员中添加接收人"));
    }

    /**
     * @param request 待试发的卡片及飞书接收人
     * @throws BusinessException 飞书未确认接收
     */
    public void verifyCard(NewsFeishuCardTestRequest request) {
        String content = digestService.previewCardContent(request.getCard(), request.getContent());
        appClient.cardSender(authorizationCredentials(request)).accept(request.getOpenId(), content);
    }

    /** @return 已保存的多人日报配置；飞书停用或未选中时目标为空，未保存时返回空值 */
    public Optional<NewsFeishuDigestDelivery> cardDigestDelivery() {
        return savedChannelConfig(mapper.selectChannel()).map(config -> {
            boolean selected = Optional.ofNullable(config.getContent())
                    .map(content -> content.getDeliveryChannels().contains(NewsDigestDeliveryChannel.FEISHU))
                    .orElse(true);
            if (!config.isEnabled() || !selected) {
                return new NewsFeishuDigestDelivery(config.getCard(), config.getContent(), List.of());
            }
            return delivery(config, config.getCard(), config.getContent(), config.getRecipients());
        });
    }

    /**
     * @param plan 已编辑的推送计划
     * @throws BusinessException 未选中有效的应用或接收人
     */
    public void validatePlanDestination(NewsPushPlanDefinition plan) {
        var config = channelConfig(mapper.selectChannel());
        validatePlanDestination(config, plan);
    }

    /** 校验计划中每位接收人和所选应用仍保存在渠道配置中。 */
    private void validatePlanDestination(NewsFeishuChannelConfig config, NewsPushPlanDefinition plan) {
        if (config.getApps().stream().noneMatch(app -> app.getAppId().equals(plan.getAppId()))) {
            throw new BusinessException("请选择已保存的飞书应用");
        }
        if (planRecipients(config, plan).size() != plan.getRecipientOpenIds().size()) {
            throw new BusinessException("请选择该应用下已配置的推送人员，每人只能选择一次");
        }
    }

    /**
     * @param plan 已保存的推送计划
     * @return 本次投递的卡片和仍有效的计划接收人
     */
    public NewsFeishuDigestDelivery planDigestDelivery(NewsPushPlanDefinition plan) {
        var config = channelConfig(mapper.selectChannel());
        validatePlanDestination(config, plan);
        var recipients = config.isEnabled() ? planRecipients(config, plan) : List.<NewsFeishuChannelConfig.Recipient>of();
        return delivery(config, planCard(config, plan), plan.getContent(), recipients);
    }

    /**
     * @param request 计划草稿及其中一位接收人
     * @throws BusinessException 目标无效、没有可预览资讯或飞书拒绝发送
     */
    public void verifyPlan(NewsPushPlanPreviewRequest request) {
        var plan = request.getDefinition();
        var config = channelConfig(mapper.selectChannel());
        validatePlanDestination(config, plan);
        if (!plan.getRecipientOpenIds().contains(request.getOpenId())) {
            throw new BusinessException("请选择该推送计划中的接收人");
        }
        var recipients = planRecipients(config, plan).stream()
                .filter(person -> person.getOpenId().equals(request.getOpenId())).collect(Collectors.toList());
        var card = planCard(config, plan);
        String content = digestService.previewCardContent(card, plan.getContent());
        delivery(config, card, plan.getContent(), recipients).getTargets().forEach(target -> target.getSendCard().accept(content));
    }

    /** @return 计划使用的已保存卡片或内置日报卡片 */
    private NewsFeishuCardSettings planCard(NewsFeishuChannelConfig config, NewsPushPlanDefinition plan) {
        if (plan.getCard() == NewsPushPlanCard.SAVED) return config.getCard();
        return new NewsFeishuCardSettings();
    }

    /** @return 仍在指定应用的推送名单中且被计划选中的接收人 */
    private List<NewsFeishuChannelConfig.Recipient> planRecipients(NewsFeishuChannelConfig config, NewsPushPlanDefinition plan) {
        return config.getRecipients().stream().filter(recipient -> recipient.getAppId().equals(plan.getAppId()))
                .filter(recipient -> plan.getRecipientOpenIds().contains(recipient.getOpenId()))
                .collect(Collectors.toList());
    }

    /** @return 使用各目标所属应用发送的日报投递任务 */
    private NewsFeishuDigestDelivery delivery(NewsFeishuChannelConfig config, NewsFeishuCardSettings card,
                                              NewsDigestContentSettings scope,
                                              List<NewsFeishuChannelConfig.Recipient> recipients) {
        Map<String, BiConsumer<String, String>> senders = config.getApps().stream()
                .collect(Collectors.toMap(NewsFeishuChannelConfig.App::getAppId, app -> appClient.cardSender(
                        new FeishuAppRobotClient.AppCredentials(app.getAppId(), app.getAppSecret()))));
        var targets = recipients.stream().filter(recipient -> senders.containsKey(recipient.getAppId()))
                .map(recipient -> new NewsFeishuDigestDelivery.Target(recipient.getAppId(), recipient.getOpenId(),
                        content -> senders.get(recipient.getAppId()).accept(recipient.getOpenId(), content)))
                .collect(Collectors.toList());
        return new NewsFeishuDigestDelivery(card, scope, targets);
    }

    /** @return 管理页面首次打开时的已保存配置或现有单应用配置 */
    private NewsFeishuChannelConfig channelConfig(NewsFeishuChannelConfigPO row) {
        return savedChannelConfig(row).orElseGet(() -> {
            var config = new NewsFeishuChannelConfig();
            Optional.ofNullable(mapper.select()).ifPresent(setting -> {
                config.setApps(List.of(NewsFeishuChannelConvert.INSTANCE.toLegacyApp(setting)));
                config.setRecipients(List.of(NewsFeishuChannelConvert.INSTANCE.toLegacyRecipient(setting)));
                config.setEnabled(setting.isEnabled());
            });
            return config;
        });
    }

    /** @return 已保存的多人通知配置 */
    private Optional<NewsFeishuChannelConfig> savedChannelConfig(NewsFeishuChannelConfigPO row) {
        return Optional.ofNullable(row).map(NewsFeishuChannelConfigPO::getConfigJson)
                .map(json -> JacksonUtil.readValue(json, NewsFeishuChannelConfig.class));
    }

    /** @return 已保存配置的并发编辑版本 */
    private long revision(NewsFeishuChannelConfigPO row) {
        return Optional.ofNullable(row).map(NewsFeishuChannelConfigPO::getRevision).orElse(0L);
    }

    /** @return 指定应用在多人配置中保存的密钥 */
    private String findAppSecret(NewsFeishuChannelConfig config, String appId) {
        return config.getApps().stream().filter(app -> app.getAppId().equals(appId)).findFirst()
                .map(NewsFeishuChannelConfig.App::getAppSecret).orElse(null);
    }

    /** @return 旧版单应用配置中对应应用的密钥 */
    private Optional<String> resolveLegacySecret(NewsFeishuAppRequest request) {
        return Optional.ofNullable(mapper.select()).filter(saved -> saved.getAppId().equals(request.getAppId()))
                .map(NewsFeishuNotificationSettingPO::getAppSecret);
    }

    /**
     * @param request 已保存或正在编辑的应用凭证
     * @return 飞书应用机器人的名称和头像
     * @throws BusinessException 缺少应用密钥或飞书查询失败
     */
    public NewsFeishuBotProfileVO botProfile(NewsFeishuAppRequest request) {
        return NewsFeishuNotificationSettingConvert.INSTANCE.toBotProfile(
                appClient.getBotProfile(authorizationCredentials(request)));
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
        Supplier<NewsFeishuNotificationSettingPO> saved = Suppliers.memoize(savedSetting::get);
        String secret = resolveSecret(request, saved);
        Optional<NewsFeishuNotificationSettingPO> recipient = Optional.ofNullable(saved.get())
                .filter(setting -> setting.getAppId().equals(request.getAppId())
                        && setting.getOpenId().equals(request.getOpenId()));
        return NewsFeishuNotificationSettingConvert.INSTANCE.toConfig(request, secret,
                Optional.ofNullable(request.getRecipientName())
                        .orElseGet(() -> recipient.map(NewsFeishuNotificationSettingPO::getRecipientName).orElse(null)),
                Optional.ofNullable(request.getRecipientAvatarUrl()).orElseGet(
                        () -> recipient.map(NewsFeishuNotificationSettingPO::getRecipientAvatarUrl).orElse(null)));
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
