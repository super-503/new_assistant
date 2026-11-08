package com.zengame.ai.business.news.feed.domain.vo;

import com.zengame.ai.business.news.domain.enums.NewsQrLoginStatus;
import lombok.Data;

/**
 * 扫码登录一次轮询的结果。
 */
@Data
public class QrLoginPollVO {

    /**
     * 扫码状态：WAITING 等待扫码 / SCANNED 已扫码待确认 / CONFIRMED 登录成功，凭证已保存 /
     * EXPIRED 二维码已过期，需重新发起
     */
    private NewsQrLoginStatus status;

    /**
     * 扫码状态的中文名
     */
    private String statusDesc;

    /**
     * CONFIRMED 时为登录账号；其余状态为空
     */
    private String account;
}
