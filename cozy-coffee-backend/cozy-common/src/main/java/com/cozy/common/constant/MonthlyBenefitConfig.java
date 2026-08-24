package com.cozy.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员月度权益配置（单一事实源，@ConfigurationProperties + application.yml）。
 * 前缀 cozy.member.monthly-benefit.levels，key 为等级（basic/silver/gold/diamond/black）。
 * 供 MemberServiceImpl.getMonthlyBenefitStatus（benefitName 展示）与
 * receiveMonthlyBenefit（发券）使用；count>1 时发多张（券码加索引）。
 */
@Data
@ConfigurationProperties(prefix = "cozy.member.monthly-benefit")
public class MonthlyBenefitConfig {

    private Map<String, LevelBenefit> levels = defaultLevels();

    /** 取某等级月度权益；等级未知返回 null */
    public LevelBenefit getLevel(String level) {
        if (level == null) {
            return null;
        }
        return levels.get(level.toLowerCase());
    }

    @Data
    public static class LevelBenefit {
        /** 权益展示文案（getMonthlyBenefitStatus 返回前端） */
        private String benefitName;
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

    private static Map<String, LevelBenefit> defaultLevels() {
        Map<String, LevelBenefit> map = new LinkedHashMap<>();
        map.put("basic", benefit("免费加浓缩券×1",
                grant("SHOT", 0, 5, 30, 1)));
        map.put("silver", benefit("配送费抵扣券×1 + 加浓缩券×2",
                grant("DELIVERY_FEE", 0, 6, 30, 1),
                grant("SHOT", 0, 5, 30, 2)));
        map.put("gold", benefit("BOGO×1 + 8.8折券×2 + 配送费抵扣券×2",
                grant("BOGO", 0, 40, 30, 1),
                grant("DISCOUNT", 0, 88, 30, 2),
                grant("DELIVERY_FEE", 0, 6, 30, 2)));
        map.put("diamond", benefit("免单券×1(限标准杯，排除特调/SOE) + BOGO×2 + 配送费抵扣券×5 + 新品5折券×1",
                grant("MONTHLY_DIAMOND_FREE", 0, 40, 30, 1),
                grant("BOGO", 0, 40, 30, 2),
                grant("DELIVERY_FEE", 0, 6, 30, 5),
                grant("NEW_PRODUCT_HALF", 0, 50, 30, 1)));
        // 黑金「无限免运费」为系统自动（下单免配送费），不发券
        map.put("black", benefit("免单券×2(全品类，不限杯型) + BOGO×5 + 无限免运费 + 新品免费券×1",
                grant("MONTHLY_BLACK_FREE", 0, 40, 30, 2),
                grant("BOGO", 0, 40, 30, 5),
                grant("NEW_PRODUCT_FREE", 0, 40, 30, 1)));
        return map;
    }

    private static LevelBenefit benefit(String benefitName, CouponGrant... coupons) {
        LevelBenefit b = new LevelBenefit();
        b.setBenefitName(benefitName);
        for (CouponGrant c : coupons) {
            b.getCoupons().add(c);
        }
        return b;
    }

    private static CouponGrant grant(String type, double min, double discount, int days, int count) {
        CouponGrant c = new CouponGrant();
        c.setCouponType(type);
        c.setMinAmount(min);
        c.setDiscountAmount(discount);
        c.setValidDays(days);
        c.setCount(count);
        return c;
    }
}
