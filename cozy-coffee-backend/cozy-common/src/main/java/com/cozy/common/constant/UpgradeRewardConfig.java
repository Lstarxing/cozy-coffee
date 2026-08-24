package com.cozy.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员晋升礼配置（单一事实源，@ConfigurationProperties + application.yml）。
 * 前缀 cozy.member.upgrade.levels，key 为等级（silver/gold/diamond/black）。
 * 供 MemberServiceImpl.grantUpgradeReward 使用（升级到某等级发放礼包：券 + 一次性积分）。
 */
@Data
@ConfigurationProperties(prefix = "cozy.member.upgrade")
public class UpgradeRewardConfig {

    private Map<String, LevelUpgrade> levels = defaultLevels();

    /** 取某等级晋升礼；等级未知返回 null */
    public LevelUpgrade getLevel(String level) {
        if (level == null) {
            return null;
        }
        return levels.get(level.toLowerCase());
    }

    @Data
    public static class LevelUpgrade {
        /** 晋升一次性积分（黑金 1688 等） */
        private int points;
        private List<CouponGrant> coupons = new ArrayList<>();
    }

    @Data
    public static class CouponGrant {
        private String couponType;
        /** 使用门槛 */
        private double minAmount;
        /** 优惠金额/封顶 */
        private double discountAmount;
        private int validDays;
        /** 发放张数（默认 1） */
        private int count = 1;
    }

    private static Map<String, LevelUpgrade> defaultLevels() {
        Map<String, LevelUpgrade> map = new LinkedHashMap<>();
        map.put("silver", upgrade(0, grant("UPGRADE_SILVER_DISCOUNT", 0, 50, 60)));
        map.put("gold", upgrade(50,
                grant("UPGRADE_GOLD_BOGO", 0, 40, 60)));
        map.put("diamond", upgrade(100,
                grant("UPGRADE_DIAMOND_STANDARD_FREE", 0, 40, 60)));
        map.put("black", upgrade(1688,
                grant("UPGRADE_BLACK_PREMIUM", 0, 999, 60)));
        return map;
    }

    private static LevelUpgrade upgrade(int points, CouponGrant... coupons) {
        LevelUpgrade u = new LevelUpgrade();
        u.setPoints(points);
        for (CouponGrant c : coupons) {
            u.getCoupons().add(c);
        }
        return u;
    }

    private static CouponGrant grant(String type, double min, double discount, int days) {
        CouponGrant c = new CouponGrant();
        c.setCouponType(type);
        c.setMinAmount(min);
        c.setDiscountAmount(discount);
        c.setValidDays(days);
        return c;
    }
}
