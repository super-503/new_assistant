package com.zengame.ai.business.news.feed.domain.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 轮询一次扫码结果。
 */
@Data
public class QrLoginPollRequest {

    /**
     * 发起扫码时返回的扫码会话标识
     */
    @NotBlank(message = "扫码会话标识不能为空")
    private String sessionId;
}
