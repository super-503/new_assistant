package com.zengame.ai.task;

import com.zengame.ai.business.news.library.NewsDigestService;
import com.zengame.ai.business.news.library.NewsFeishuDigestService;
import com.zengame.ai.business.news.setting.NewsFeishuNotificationSettingService;
import com.zengame.ai.repository.ai.mapper.NewsPushPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/** 按配置的时区和计划发送个人资讯日报。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsDigestTask {

    private final NewsFeishuNotificationSettingService notificationSettings;
    private final NewsDigestService newsDigestService;
    private final NewsFeishuDigestService feishuDigestService;
    private final NewsPushPlanMapper pushPlans;

    /** 有已启用的通知目标时，分批发送待发资讯。 */
    @Scheduled(cron = "${news.digest-cron}", zone = "${news.time-zone}")
    public void sendDigest() {
        if (pushPlans.existsPlans()) return;
        notificationSettings.cardDigestDelivery().ifPresentOrElse(delivery -> {
            if (delivery.getTargets().isEmpty()) return;
            while (feishuDigestService.sendBatch(delivery)) {
                log.info("飞书日报卡片批次已发送");
            }
        }, () -> notificationSettings.digestSender().ifPresent(this::send));
    }

    private void send(Consumer<String> sender) {
        int count = newsDigestService.sendBatch(sender);
        while (count > 0) {
            log.info("个人资讯日报已发送, count={}", count);
            count = newsDigestService.sendBatch(sender);
        }
    }
}
