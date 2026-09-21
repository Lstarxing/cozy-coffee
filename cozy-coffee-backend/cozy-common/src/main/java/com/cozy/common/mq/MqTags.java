package com.cozy.common.mq;

/**
 * RocketMQ 标签常量。
 * ORDER_CREATED 只负责清管理端缓存（下单不提醒商家，避免弃单噪声）；
 * ORDER_PAID 才是「商家该开始做这一单」的信号，由网关在用户侧接单（= 支付）成功后发出。
 */
public final class MqTags {

    private MqTags() {
    }

    public static final String ORDER_CREATED = "order_created";
    public static final String ORDER_PAID = "order_paid";
    public static final String ORDER_COMPLETED = "order_completed";
    public static final String ORDER_CANCELLED = "order_cancelled";

    public static final String COUPON_GRANT_REQUESTED = "coupon_grant_requested";

    public static final String USER_CREATED = "user_created";
    public static final String WELCOME_GIFT_ELIGIBLE = "welcome_gift_eligible";
    public static final String PROFILE_COMPLETED = "profile_completed";
    public static final String BIRTHDAY_SET = "birthday_set";
    public static final String INVITE_REWARD_EARNED = "invite_reward_earned";
}
