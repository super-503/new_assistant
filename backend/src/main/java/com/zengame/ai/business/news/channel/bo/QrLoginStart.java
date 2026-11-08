package com.zengame.ai.business.news.channel.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发起扫码登录的结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrLoginStart {

    /**
     * 插件自定义的扫码会话状态（不透明字符串，如平台登录流程中间需要的 cookie 和二维码标识），
     * 由核心层存入 Redis，轮询时原样传回
     */
    private String sessionState;

    /**
     * 二维码图片，data URL 格式（data:image/png;base64,...），前端直接作为 img 的 src
     */
    private String qrImage;

    /**
     * 二维码有效期（秒），与平台二维码一致，也是扫码会话在 Redis 的有效期
     */
    private int expireSeconds;
}
