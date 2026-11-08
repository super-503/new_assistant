package com.zengame.ai.repository.ai.mapper;

import com.zengame.ai.repository.ai.po.NewsAiModelSettingPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NewsAiModelSettingMapper {

    NewsAiModelSettingPO select();

    /** 保存全局配置。密钥为空值且地址一致时保留当前密钥。 */
    int upsert(@Param("setting") NewsAiModelSettingPO setting, @Param("initialApiKey") String initialApiKey);
}
