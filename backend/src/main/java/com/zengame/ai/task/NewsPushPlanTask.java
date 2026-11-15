package com.zengame.ai.task;

import com.zengame.ai.business.news.setting.NewsPushPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定期执行到期的资讯推送计划。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsPushPlanTask {
    private static final int BATCHES_PER_POLL = 10;
    private final NewsPushPlanService plans;

    /** 推送当前到期的计划；每次调度为各计划分配有限的批次数。 */
    @Scheduled(cron = "${news.push-plan-cron:*/30 * * * * *}")
    public void sendDuePlans() {
        for (long id : plans.due()) {
            try {
                for (int batch = 0; batch < BATCHES_PER_POLL; batch++) {
                    if (!plans.runDueBatch(id)) break;
                }
            } catch (RuntimeException e) {
                log.error("执行资讯推送计划失败, planId={}", id, e);
            }
        }
    }
}
