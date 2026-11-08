package com.zengame.ai.business.news.channel.x.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 一条推文：作者、正文、实体、媒体和引用帖。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XPost {

    /**
     * 推文 rest_id
     */
    private String restId;

    /**
     * 作者显示名称
     */
    private String authorName;

    /**
     * 作者用户名，不带 @
     */
    private String authorScreenName;

    /**
     * 作者原尺寸头像地址
     */
    private String authorAvatarUrl;

    /**
     * 发帖时间
     */
    private LocalDateTime createdAt;

    /**
     * 纯文本正文（长推文取完整正文），HTML 实体已还原
     */
    private String text;

    /**
     * 正文里的 t.co 短链
     */
    private List<XUrlEntity> urls;

    /**
     * 正文里 @提及的用户名，不带 @
     */
    private List<String> mentions;

    /**
     * 正文里的话题，不带 #
     */
    private List<String> hashtags;

    /**
     * 图片、视频和 GIF，按推文中的顺序
     */
    private List<XMedia> media;

    /**
     * 引用的推文；不是引用帖或引用帖不可见时为空
     */
    private Optional<XPost> quoted;

    /**
     * 点赞数
     */
    private Long favoriteCount;

    /**
     * 回复数
     */
    private Long replyCount;

    /**
     * @return 推文链接，如 https://x.com/OpenAI/status/2117307611338997846
     */
    public String getPermalink() {
        return "https://x.com/" + authorScreenName + "/status/" + restId;
    }
}
