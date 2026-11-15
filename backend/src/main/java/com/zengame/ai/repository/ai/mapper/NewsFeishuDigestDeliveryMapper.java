package com.zengame.ai.repository.ai.mapper;

import com.zengame.ai.business.news.setting.domain.bo.NewsFeishuDigestDelivery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NewsFeishuDigestDeliveryMapper {
    List<Long> selectDelivered(@Param("appId") String appId, @Param("openId") String openId,
                               @Param("itemIds") List<Long> itemIds);

    int markDelivered(@Param("appId") String appId, @Param("openId") String openId,
                      @Param("itemIds") List<Long> itemIds);

    int deleteByItemIds(@Param("itemIds") List<Long> itemIds);

    List<Long> selectPlanDelivered(@Param("planId") long planId, @Param("appId") String appId,
                                  @Param("openId") String openId, @Param("itemIds") List<Long> itemIds);

    int markPlanDelivered(@Param("planId") long planId, @Param("appId") String appId,
                          @Param("openId") String openId, @Param("itemIds") List<Long> itemIds);

    int markPlanItemsCompleted(@Param("planId") long planId, @Param("itemIds") List<Long> itemIds);

    int deletePlanDelivered(@Param("planId") long planId, @Param("itemIds") List<Long> itemIds);

    /** 将已送达当前全部接收人的资讯记为计划已完成。 */
    int completePlanItems(@Param("planId") long planId,
                          @Param("targets") List<NewsFeishuDigestDelivery.Target> targets);

    /** 清理已完成计划资讯的逐人回执。 */
    int deleteCompletedPlanDelivered(@Param("planId") long planId);
}
