package com.zengame.ai.task;

import com.zengame.ai.business.news.channel.ChannelPullService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 按配置的时区和计划拉取资讯。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsPullTask {

    private final ChannelPullService channelPullService;

    /** 拉取启用的渠道，将新资讯交给数据加工。 */
    @Scheduled(cron = "${news.pull-cron}", zone = "${news.time-zone}")
    public void pullAll() {
        log.info("开始拉取资讯");
        channelPullService.pullAll();
        log.info("资讯拉取完成");
    }
}
