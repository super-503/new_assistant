package com.zengame.ai.business.news.setting.cover;

import com.zengame.ai.business.news.setting.domain.bo.NewsPushPlanDefinition;
import com.zengame.ai.business.news.setting.domain.vo.NewsPushPlanVO;
import com.zengame.ai.infrastructure.util.JacksonUtil;
import com.zengame.ai.repository.ai.po.NewsPushPlanPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/** 推送计划数据库记录与管理端展示之间的转换。 */
@Mapper
public interface NewsPushPlanConvert {
    NewsPushPlanConvert INSTANCE = Mappers.getMapper(NewsPushPlanConvert.class);

    /** @return 可显示和编辑的推送计划 */
    @Mapping(target = "definition", source = "configJson", qualifiedByName = "readDefinition")
    NewsPushPlanVO toVO(NewsPushPlanPO plan);

    /** @return 计划保存的内容、发送目标和时刻 */
    @Named("readDefinition")
    default NewsPushPlanDefinition readDefinition(String json) {
        return JacksonUtil.readValue(json, NewsPushPlanDefinition.class);
    }
}
