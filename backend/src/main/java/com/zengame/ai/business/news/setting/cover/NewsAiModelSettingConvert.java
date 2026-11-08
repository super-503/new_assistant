package com.zengame.ai.business.news.setting.cover;

import com.zengame.ai.business.news.setting.domain.bo.NewsAiModelConfig;
import com.zengame.ai.business.news.setting.domain.vo.NewsAiModelSettingVO;
import com.zengame.ai.repository.ai.po.NewsAiModelSettingPO;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/** 转换模型配置及可供设置页查看的状态。 */
@Mapper
public interface NewsAiModelSettingConvert {

    NewsAiModelSettingConvert INSTANCE = Mappers.getMapper(NewsAiModelSettingConvert.class);

    /** 从存储记录读取完整模型配置。 */
    NewsAiModelConfig toConfig(NewsAiModelSettingPO setting);

    /** 将模型配置转换为持久化记录。 */
    NewsAiModelSettingPO toPO(NewsAiModelConfig config);

    /** 返回已配置的模型地址、名称和密钥状态。 */
    @Mapping(target = "configured", constant = "true")
    @Mapping(target = "apiKeyConfigured", source = "apiKey", qualifiedByName = "hasApiKey")
    NewsAiModelSettingVO toVO(NewsAiModelConfig config);

    @Named("hasApiKey")
    default boolean hasApiKey(String apiKey) {
        return StringUtils.isNotBlank(apiKey);
    }
}
