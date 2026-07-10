package com.cozy.gateway.util;

import com.cozy.common.context.UserContext;
import com.cozy.gateway.exception.UnauthorizedException;

/**
 * 认证工具 — 统一从 UserContext 提取 userId，未登录时抛出 UnauthorizedException。
 */
public final class AuthUtil {

    private AuthUtil() {
    }

    public static Long requireUserId() {
        Long userId = UserContext.getUserIdOrNull();
        if (userId == null) {
            throw new UnauthorizedException();
        }
        return userId;
    }
}
