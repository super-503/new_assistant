package com.zengame.ai.business.news.channel;

import com.google.common.collect.Sets;
import com.zengame.ai.business.news.channel.bo.ChannelDescriptor;
import com.zengame.ai.business.news.domain.enums.NewsCredentialMode;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.NewsChannelMapper;
import com.zengame.ai.repository.ai.po.NewsChannelPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 渠道插件注册表：收集所有 {@link ChannelPlugin} Bean，按渠道标识分发。
 *
 * <p>启动时校验两件事，不满足即启动失败：
 * <ul>
 *     <li>插件声明的凭证方式与实现的能力接口一致，渠道标识不重复；</li>
 *     <li>应用启动完成时，news_channel 表的渠道与已注册插件一一对应，任一方多出都不允许。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelPluginRegistry {

    private final List<ChannelPlugin> plugins;

    private final NewsChannelMapper newsChannelMapper;

    private Map<String, ChannelPlugin> pluginByCode;

    /**
     * 按渠道标识索引插件，并校验插件的凭证方式与能力接口一致。
     *
     * @throws IllegalStateException 渠道标识为空或重复，或凭证方式与能力接口不一致
     */
    @PostConstruct
    public void indexPlugins() {
        Map<String, ChannelPlugin> indexed = new LinkedHashMap<>();
        for (ChannelPlugin plugin : plugins) {
            ChannelDescriptor descriptor = plugin.descriptor();
            if (StringUtils.isBlank(descriptor.getCode())) {
                throw new IllegalStateException("资讯渠道插件未声明渠道标识: " + plugin.getClass().getName());
            }
            checkCredentialCapability(plugin, descriptor);
            ChannelPlugin duplicate = indexed.putIfAbsent(descriptor.getCode(), plugin);
            if (duplicate != null) {
                throw new IllegalStateException(String.format("资讯渠道标识重复: %s，插件 %s 与 %s",
                        descriptor.getCode(), duplicate.getClass().getName(), plugin.getClass().getName()));
            }
        }
        pluginByCode = Collections.unmodifiableMap(indexed);
    }

    /**
     * 应用启动完成时比对 news_channel 表与已注册插件。
     *
     * @throws IllegalStateException 有渠道没有插件，或有插件没有渠道记录
     */
    @EventListener(ApplicationReadyEvent.class)
    public void verifyChannelTable() {
        Set<String> channelCodes = newsChannelMapper.selectAll().stream()
                .map(NewsChannelPO::getCode)
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> pluginCodes = new TreeSet<>(pluginByCode.keySet());
        Set<String> channelsWithoutPlugin = Sets.difference(channelCodes, pluginCodes);
        Set<String> pluginsWithoutChannel = Sets.difference(pluginCodes, channelCodes);
        if (!channelsWithoutPlugin.isEmpty() || !pluginsWithoutChannel.isEmpty()) {
            throw new IllegalStateException(String.format("资讯渠道与插件不一致：没有插件的渠道 %s，没有渠道记录的插件 %s",
                    channelsWithoutPlugin, pluginsWithoutChannel));
        }
        log.info("资讯渠道插件注册完成, channels={}", pluginCodes);
    }

    /**
     * @param channelCode 渠道标识
     * @return 渠道插件
     * @throws BusinessException 渠道不存在
     */
    public ChannelPlugin getPlugin(String channelCode) {
        return Optional.ofNullable(pluginByCode.get(channelCode))
                .orElseThrow(() -> new BusinessException("资讯渠道不存在: " + channelCode));
    }

    /**
     * 取渠道插件的某项能力，渠道不具备时为空。用于按能力分支的流程，如拉取前的凭证续期。
     *
     * @param channelCode 渠道标识
     * @param capability  能力接口，如 {@link CredentialRefreshable}、{@link QrLoginCapable}、{@link TokenCredential}、
     *                    {@link CredentialVerifiable}
     * @param <T>         能力接口类型
     * @return 以能力接口类型返回的插件；不具备该能力时为空
     * @throws BusinessException 渠道不存在
     */
    public <T> Optional<T> findCapability(String channelCode, Class<T> capability) {
        return Optional.of(getPlugin(channelCode))
                .filter(capability::isInstance)
                .map(capability::cast);
    }

    /**
     * 取渠道插件的某项能力，渠道必须具备。用于只对特定渠道开放的操作，如发起扫码、保存 Token。
     *
     * @param channelCode 渠道标识
     * @param capability  能力接口
     * @param <T>         能力接口类型
     * @return 以能力接口类型返回的插件
     * @throws BusinessException 渠道不存在或不具备该能力
     */
    public <T> T getCapability(String channelCode, Class<T> capability) {
        return findCapability(channelCode, capability)
                .orElseThrow(() -> new BusinessException(
                        String.format("资讯渠道 %s 不支持 %s", channelCode, capability.getSimpleName())));
    }

    /**
     * 凭证方式为 QR 的插件必须实现 {@link QrLoginCapable}，为 TOKEN 的必须实现 {@link TokenCredential}，
     * 为 NONE 的两者都不能实现。
     */
    private void checkCredentialCapability(ChannelPlugin plugin, ChannelDescriptor descriptor) {
        NewsCredentialMode mode = descriptor.getCredentialMode();
        boolean qr = plugin instanceof QrLoginCapable;
        boolean token = plugin instanceof TokenCredential;
        boolean consistent = (mode == NewsCredentialMode.QR && qr && !token)
                || (mode == NewsCredentialMode.TOKEN && token && !qr)
                || (mode == NewsCredentialMode.NONE && !qr && !token);
        if (!consistent) {
            throw new IllegalStateException(String.format("资讯渠道 %s 的凭证方式 %s 与实现的能力接口不一致，插件 %s",
                    descriptor.getCode(), mode, plugin.getClass().getName()));
        }
    }
}
