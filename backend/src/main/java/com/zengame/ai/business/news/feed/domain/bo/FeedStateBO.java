package com.zengame.ai.business.news.feed.domain.bo;

import com.zengame.ai.business.news.domain.enums.NewsCredentialStatus;
import com.zengame.ai.business.news.domain.enums.NewsFeedPullStatus;
import com.zengame.ai.business.news.feed.domain.enums.NewsFeedStatus;
import com.zengame.ai.repository.ai.po.NewsFeedPO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;

/**
 * 订阅源的展示状态和失败原因，展示时计算，不写库。
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FeedStateBO {

    /**
     * 渠道凭证失效时，启用中的订阅源展示的失败原因
     */
    private static final String CREDENTIAL_INVALID_ERROR = "渠道凭证失效";

    /**
     * 展示状态
     */
    private final NewsFeedStatus status;

    /**
     * 拉取失败的原因；状态不是拉取失败时为空
     */
    private final String error;

    /**
     * 推导订阅源的展示状态：已停用优先；渠道凭证失效时启用中的订阅源为拉取失败，原因「渠道凭证失效」；
     * 其余按订阅源自己的拉取结果。
     *
     * @param feed             订阅源
     * @param credentialStatus 所属渠道的凭证状态
     * @return 展示状态和失败原因
     */
    public static FeedStateBO resolve(NewsFeedPO feed, NewsCredentialStatus credentialStatus) {
        if (BooleanUtils.isFalse(feed.getEnabled())) {
            return new FeedStateBO(NewsFeedStatus.DISABLED, null);
        }
        if (credentialStatus == NewsCredentialStatus.INVALID) {
            return new FeedStateBO(NewsFeedStatus.ERROR, CREDENTIAL_INVALID_ERROR);
        }
        if (feed.getPullStatus() == NewsFeedPullStatus.ERROR) {
            return new FeedStateBO(NewsFeedStatus.ERROR, feed.getPullError());
        }
        if (feed.getPullStatus() == NewsFeedPullStatus.PENDING) {
            return new FeedStateBO(NewsFeedStatus.PENDING, null);
        }
        return new FeedStateBO(NewsFeedStatus.OK, null);
    }

    /**
     * @return 是否拉取失败
     */
    public boolean isFailed() {
        return status == NewsFeedStatus.ERROR;
    }
}
