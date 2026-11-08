package com.zengame.ai.business.news.feed.domain.vo;

import lombok.Data;

/**
 * 发起扫码登录的结果，扫码弹窗据此显示二维码并开始轮询。
 */
@Data
public class QrLoginStartVO {

    /**
     * 扫码会话标识，轮询时回传
     */
    private String sessionId;

    /**
     * 二维码图片，data URL 格式，直接作为 img 的 src
     */
    private String qrImage;

    /**
     * 二维码有效期（秒）
     */
    private int expireSeconds;
}
