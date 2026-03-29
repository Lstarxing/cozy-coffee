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

    public static String lockMallProductStock(Long productId) {
        return "cozy:lock:mall:stock:" + productId;
    }

    public static String userLoginSession(String token) {
        return "cozy:auth:session:" + token;
    }

    public static String userProfileById(Long userId) {
        return "cozy:user:profile:" + userId;
    }

    public static String memberProfileByUserId(Long userId) {
        return "cozy:member:profile:" + userId;
    }
}
