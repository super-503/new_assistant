package com.zengame.ai.business.news.channel.x.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

/**
 * 用户时间线上的一条：订阅账号自己的推文，转推时带被转的原帖。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XTimelineTweet {

    /**
     * 出现在时间线上的推文（原创、回复、引用或转推本身），发帖时间即订阅源出现时间
     */
    private XPost tweet;

    /**
     * 被转的原帖；不是转推时为空
     */
    private Optional<XPost> retweeted;

    /**
     * @return 入库使用的内容：转推为被转的原帖，其余为推文本身
     */
    public XPost getContent() {
        return retweeted.orElse(tweet);
    }
}
