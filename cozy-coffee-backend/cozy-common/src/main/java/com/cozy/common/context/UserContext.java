package com.cozy.common.context;

/**
 * 用户上下文持有者
 * 使用 ThreadLocal 存储当前请求的用户信息，在整个请求链中共享
 */
public class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER_ROLE = new ThreadLocal<>();

    /**
     * 设置当前用户ID（由拦截器调用）
     */
    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /**
     * 设置当前用户角色（由拦截器调用）
     */
    public static void setRole(String role) {
        CURRENT_USER_ROLE.set(role);
    }

    /**
     * 获取当前用户ID（业务层调用）
     */
    public static Long getUserId() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        return userId;
    }

    /**
     * 获取当前用户ID，如果未登录返回 null
     */
    public static Long getUserIdOrNull() {
        return CURRENT_USER_ID.get();
    }

    /**
     * 获取当前用户角色
     */
    public static String getRole() {
        String role = CURRENT_USER_ROLE.get();
        return role != null ? role : "user";
    }

    /**
     * 判断当前用户是否为管理员
     */
    public static boolean isAdmin() {
        return "admin".equals(getRole());
    }

    /**
     * 清除用户上下文（请求结束时调用）
     */
    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_USER_ROLE.remove();
    }
}
