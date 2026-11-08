package com.zengame.ai.business.news.setting;

import com.zengame.ai.apiclient.model.NewsTextClient;
import com.zengame.ai.business.news.setting.cover.NewsAiModelSettingConvert;
import com.zengame.ai.business.news.setting.domain.bo.NewsAiModelConfig;
import com.zengame.ai.business.news.setting.domain.request.NewsAiModelSaveRequest;
import com.zengame.ai.business.news.setting.domain.vo.NewsAiModelSettingVO;
import com.zengame.ai.config.properties.NewsProperties;
import com.zengame.ai.infrastructure.exception.BusinessException;
import com.zengame.ai.repository.ai.mapper.NewsAiModelSettingMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Optional;

/** 资讯模型的管理与运行时配置读取。 */
@Service
@RequiredArgsConstructor
public class NewsAiModelSettingService {

    private static final String CONFIG_REQUIRED = "请先在「基础配置 → AI 模型」中保存模型配置";
    private static final NewsAiModelSettingVO UNCONFIGURED = new NewsAiModelSettingVO(false, "", "", false);

    private final NewsAiModelSettingMapper mapper;
    private final NewsProperties properties;
    private final NewsTextClient client;

    /** @return 当前模型；没有可用配置时为空 */
    public Optional<NewsAiModelConfig> current() {
        return Optional.ofNullable(mapper.select())
                .map(NewsAiModelSettingConvert.INSTANCE::toConfig)
                .or(this::configuredDefault);
    }

    /** @return 是否已设置模型地址和名称 */
    public boolean isConfigured() {
        return current().isPresent();
    }

    /** @return 当前模型，未配置时提示管理员设置 */
    public NewsAiModelConfig requireConfigured() {
        return current().orElseThrow(() -> new BusinessException(CONFIG_REQUIRED));
    }

    /** @return 模型设置页的地址、名称和密钥状态 */
    public NewsAiModelSettingVO get() {
        return current().map(NewsAiModelSettingConvert.INSTANCE::toVO).orElse(UNCONFIGURED);
    }

    /**
     * 保存用于后续资讯加工的模型配置。
     * @param request 模型地址、名称和可选密钥
     * @return 保存后的配置状态
     * @throws BusinessException 模型地址或密钥格式无效
     */
    public NewsAiModelSettingVO save(NewsAiModelSaveRequest request) {
        NewsAiModelConfig config = validate(request);
        String initialApiKey = configuredDefault().filter(value -> value.getModelUrl().equals(config.getModelUrl()))
                .map(NewsAiModelConfig::getApiKey).orElse(null);
        mapper.upsert(NewsAiModelSettingConvert.INSTANCE.toPO(config), initialApiKey);
        return get();
    }

    /**
     * 用表单配置发送一条连接测试消息。
     * @param request 待测试的配置
     * @throws BusinessException 配置无效，或模型接口未返回有效回复
     */
    public void verify(NewsAiModelSaveRequest request) {
        NewsAiModelConfig draft = validate(request);
        String apiKey = Optional.ofNullable(draft.getApiKey()).orElseGet(() -> current()
                .filter(value -> value.getModelUrl().equals(draft.getModelUrl()))
                .map(NewsAiModelConfig::getApiKey).orElse(null));
        client.complete(new NewsAiModelConfig(draft.getModelUrl(), draft.getModel(), apiKey), "Reply with OK.");
    }

    /** 校验表单中的接口地址与密钥，返回可用于调用的模型配置。 */
    private NewsAiModelConfig validate(NewsAiModelSaveRequest request) {
        String modelUrl = validUrl(request.getModelUrl())
                .orElseThrow(() -> new BusinessException("请填写完整的 HTTP 或 HTTPS 模型接口地址"));
        String apiKey = StringUtils.trim(request.getApiKey());
        if (StringUtils.containsAny(apiKey, '\r', '\n')) {
            throw new BusinessException("API 密钥不能包含换行符");
        }
        return new NewsAiModelConfig(modelUrl, StringUtils.trim(request.getModel()), apiKey);
    }

    /** 读取部署配置中的默认模型。 */
    private Optional<NewsAiModelConfig> configuredDefault() {
        return validUrl(properties.getModelUrl())
                .filter(url -> StringUtils.isNotBlank(properties.getModel()))
                .map(url -> new NewsAiModelConfig(url, StringUtils.trim(properties.getModel()),
                        StringUtils.trim(properties.getModelApiKey())));
    }

    /** 接口地址使用 HTTP(S)，凭据通过单独的密钥项提供。 */
    private Optional<String> validUrl(String value) {
        try {
            URI uri = URI.create(StringUtils.trimToEmpty(value));
            boolean http = StringUtils.equalsAnyIgnoreCase(uri.getScheme(), "http", "https");
            if (http && uri.getHost() != null && uri.getPort() <= 65535
                    && uri.getRawUserInfo() == null && uri.getRawFragment() == null) {
                return Optional.of(uri.toASCIIString());
            }
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
