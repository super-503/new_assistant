package com.zengame.ai.business.news.setting.cover;

import com.zengame.ai.business.news.setting.domain.request.NewsRejectReasonSaveRequest;
import com.zengame.ai.business.news.setting.domain.vo.NewsRejectReasonVO;
import com.zengame.ai.repository.ai.po.NewsRejectReasonPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 拒绝理由配置的转换。
 */
@Mapper
public interface NewsRejectReasonConvert {

    NewsRejectReasonConvert INSTANCE = Mappers.getMapper(NewsRejectReasonConvert.class);

    /**
     * @param pos 拒绝理由
     * @return 展示对象，保持原顺序
     */
    List<NewsRejectReasonVO> toVOList(List<NewsRejectReasonPO> pos);

    /**
     * @param po 拒绝理由
     * @return 展示对象
     */
    NewsRejectReasonVO toVO(NewsRejectReasonPO po);

    /**
     * 新增请求转库表行，排序由服务层决定。
     *
     * @param request 保存请求
     * @return 库表行
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sortNo", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    NewsRejectReasonPO toPO(NewsRejectReasonSaveRequest request);

    /**
     * 编辑请求覆盖到已有的库表行，主键和排序保持不变。
     *
     * @param request 保存请求
     * @param po      已有的拒绝理由
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sortNo", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void updatePO(NewsRejectReasonSaveRequest request, @MappingTarget NewsRejectReasonPO po);
}
