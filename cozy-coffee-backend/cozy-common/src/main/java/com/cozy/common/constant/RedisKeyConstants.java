package com.cozy.common.constant;

/**
 * Redis key naming conventions for CozyCoffee.
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    public static final String ORDER_MENU_ACTIVE = "cozy:menu:coffee:active";
    public static final String MALL_PRODUCTS_ACTIVE = "cozy:mall:products:active";

    public static final String LOCK_ORDER_MENU_REBUILD = "cozy:lock:menu:coffee:rebuild";
    public static final String LOCK_MALL_PRODUCTS_REBUILD = "cozy:lock:mall:products:rebuild";
    public static final String LOCK_ORDER_TIMEOUT_CANCEL_JOB = "cozy:lock:order:timeout:cancel:job";

    public static final String ADMIN_DASHBOARD_STATS_PREFIX = "cozy:admin:dashboard:stats:";
    public static final String ADMIN_ANALYTICS_TREND_PREFIX = "cozy:admin:analytics:trend:";
    public static final String ADMIN_ANALYTICS_DISTRIBUTION_PREFIX = "cozy:admin:analytics:distribution:";
    public static final String ADMIN_ANALYTICS_RANK_PREFIX = "cozy:admin:analytics:rank:";
    public static final String ADMIN_ORDERS_LIST_PREFIX = "cozy:admin:orders:list:";

    public static String lockMallProductStock(Long productId) {
        return "cozy:lock:mall:stock:" + productId;
    }

    public static String userLoginSession(String token) {
        return "cozy:auth:session:" + token;
    }

    public static String userCurrentTokenById(Long userId) {
        return "cozy:auth:user:token:" + userId;
    }

    public static String userProfileById(Long userId) {
        return "cozy:user:profile:" + userId;
    }

    public static String memberProfileByUserId(Long userId) {
        return "cozy:member:profile:" + userId;
    }
}
