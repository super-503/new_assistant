package com.zengame.ai.business.news.feed.domain.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 运营粘贴的 Token 凭证，用于保存 Token 和校验输入框里的 Token。
 */
@Data
public class TokenSaveRequest {

    /**
     * Token，如 X 的 auth_token
     */
    @NotBlank(message = "请填写 Token")
    private String token;
}
