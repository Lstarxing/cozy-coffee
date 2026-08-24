package com.cozy.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 生日权益配置（单一事实源，@ConfigurationProperties + application.yml）。
 * 前缀 cozy.member.birthday.levels，key 为等级（basic/silver/gold/diamond/black）。
 * 供 MemberServiceImpl.grantBirthdayRewardInternal 使用（白皮书「生日礼」阶梯权益）。
 */
@Data
@ConfigurationProperties(prefix = "cozy.member.birthday")
public class BirthdayRewardConfig {

    private Map<String, LevelBirthday> levels = defaultLevels();

    /** 取某等级生日权益；等级未知返回 null */
    public LevelBirthday getLevel(String level) {
        if (level == null) {
            return null;
        }
        return levels.get(level.toLowerCase());
    }

    @Data
    public static class LevelBirthday {
        /** 生日积分（黑金 888，其余等级 0） */
        private int points;
        private List<BirthdayCoupon> coupons = new ArrayList<>();
    }

    @Data
    public static class BirthdayCoupon {
        private String couponType;
        /** 使用门槛 */
        private double minAmount;
        /** 优惠金额/封顶（DISCOUNT 券为折扣率） */
        private double discountAmount;
        private int validDays;
    }

    private static Map<String, LevelBirthday> defaultLevels() {
        Map<String, LevelBirthday> map = new LinkedHashMap<>();
        map.put("black", level(888,
                coupon("BIRTHDAY_BLACK_FREE", 0, 40, 30),
                coupon("BIRTHDAY_FREE_CAKE", 0, 40, 30)));
        map.put("diamond", level(0,
                coupon("BIRTHDAY_DIAMOND_FREE", 0, 40, 30),
                coupon("BIRTHDAY_CAKE_HALF", 0, 50, 30)));
        map.put("gold", level(0, coupon("BIRTHDAY_GOLD_FREE", 0, 40, 30)));
        map.put("silver", level(0, coupon("BIRTHDAY_SILVER_BOGO", 0, 40, 30)));
        map.put("basic", level(0, coupon("BIRTHDAY_BASIC_DISCOUNT", 0, 50, 30)));
        return map;
    }

    private static LevelBirthday level(int points, BirthdayCoupon... coupons) {
        LevelBirthday l = new LevelBirthday();
        l.setPoints(points);
        for (BirthdayCoupon c : coupons) {
            l.getCoupons().add(c);
        }
        return l;
    }

    private static BirthdayCoupon coupon(String type, double min, double discount, int days) {
        BirthdayCoupon c = new BirthdayCoupon();
        c.setCouponType(type);
        c.setMinAmount(min);
        c.setDiscountAmount(discount);
        c.setValidDays(days);
        return c;
    }
}
