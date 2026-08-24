package com.cozy.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 签到奖励配置（单一事实源，@ConfigurationProperties + application.yml）。
 * 前缀 cozy.member.signin。供 SigninServiceImpl 使用（每日签到积分 + 7 日连签券）。
 */
@Data
@ConfigurationProperties(prefix = "cozy.member.signin")
public class SigninRewardConfig {

    /** 每日签到固定积分 */
    private int dailyPoints = 2;

    /** 连续签到 N 天触发连签券奖励 */
    private int sevenDayCouponAfterDays = 7;

    /** 连签券配置 */
    private SevenDayCoupon sevenDayCoupon = new SevenDayCoupon();

    @Data
    public static class SevenDayCoupon {
        private String couponType = "SIGNIN_7DAY";
        private double minAmount = 35;
        private double discountAmount = 10;
        private int validDays = 3;
    }
}
