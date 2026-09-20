package com.cozy.common.mq;

/**
 * RocketMQ 标签常量。
 * 本期仅接入 ORDER_CREATED，其余标签保留位置以便后续扩展。
 */
public final class MqTags {

    private MqTags() {
    }

    public static final String ORDER_CREATED = "order_created";
    public static final String ORDER_COMPLETED = "order_completed";
    public static final String ORDER_CANCELLED = "order_cancelled";

    public static final String COUPON_GRANT_REQUESTED = "coupon_grant_requested";

    public static final String USER_CREATED = "user_created";
    public static final String WELCOME_GIFT_ELIGIBLE = "welcome_gift_eligible";
    public static final String PROFILE_COMPLETED = "profile_completed";
    public static final String BIRTHDAY_SET = "birthday_set";
    public static final String INVITE_REWARD_EARNED = "invite_reward_earned";
}
