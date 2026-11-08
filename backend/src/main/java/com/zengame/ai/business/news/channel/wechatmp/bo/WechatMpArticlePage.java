package com.zengame.ai.business.news.channel.wechatmp.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 从公众号文章页解析出的内容：修正后的正文和所属公众号。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WechatMpArticlePage {

    /**
     * 修正后的正文 HTML；页面没有正文区（如图片消息）时为空
     */
    private String bodyHtml;

    /**
     * 公众号名称
     */
    private String accountName;

    /**
     * 公众号头像地址
     */
    private String accountAvatarUrl;
}
