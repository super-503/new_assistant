package com.zengame.ai.repository.ai.mapper;

import com.zengame.ai.repository.ai.po.NewsPushPlanPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 推送计划和调度时间的持久化接口。 */
@Mapper
public interface NewsPushPlanMapper {
    List<NewsPushPlanPO> selectAll();

    boolean existsPlans();

    List<Long> selectDue(@Param("now") LocalDateTime now);

    NewsPushPlanPO selectForUpdate(@Param("id") long id);

    int insert(NewsPushPlanPO plan);

    int update(NewsPushPlanPO plan);

    void updateNextRunAt(@Param("id") long id, @Param("nextRunAt") LocalDateTime nextRunAt);
}
