package com.zengame.ai.infrastructure.login;

import org.springframework.security.core.context.SecurityContextHolder;

/** 读取当前资讯管理员的会话身份。 */
public final class LoginUtils {

    private LoginUtils() {
    }

    /** @return 审核或编辑操作的登录用户名 */
    public static String getUserName() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
