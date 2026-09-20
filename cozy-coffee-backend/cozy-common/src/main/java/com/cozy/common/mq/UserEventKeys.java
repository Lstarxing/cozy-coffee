package com.cozy.common.mq;

/** 用户生命周期事件的业务幂等键；生产端、outbox 与消费端必须共用同一口径。 */
public final class UserEventKeys {

    private UserEventKeys() {
    }

    public static String userCreated(Long userId) {
        return "user_created_" + userId;
    }

    public static String welcomeGift(Long userId) {
        return "NEW_USER_COUPON_" + userId;
    }

    public static String profileCompleted(Long userId) {
        return "profile_completed_" + userId;
    }

    public static String birthday(Long userId, int benefitYear) {
        return "birthday_" + userId + "_" + benefitYear;
    }

    public static String inviteReward(Long inviteeUserId, Long inviterId) {
        return "invite_firstorder_" + inviteeUserId + "_" + inviterId;
    }
}
