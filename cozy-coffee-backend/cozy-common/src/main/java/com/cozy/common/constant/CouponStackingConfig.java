package com.cozy.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 优惠券叠加（主券/辅券/独占）配置，单一事实源。
 * 前缀 cozy.mall.coupon-stacking，yml 可整体覆盖 categories/limits。
 *
 * 分类判定优先级：ruleJson.stacking（实例级快照） → categories[type] → 默认 MAIN。
 * categories 只覆盖少数类型（DELIVERY_FEE/SHOT → ADDON），其余类型缺省即 MAIN；
 * EXCLUSIVE 一般由实例级 stacking/ruleJson.exclusive=true 表达，不进类型映射。
 */
@Data
@ConfigurationProperties(prefix = "cozy.mall.coupon-stacking")
public class CouponStackingConfig {

    /** 类型 → 分类（MAIN / ADDON / EXCLUSIVE）；缺省按 MAIN 处理 */
    private Map<String, String> categories = new HashMap<>();

    /** 一单最多可用配送费券张数 */
    private Integer maxDeliveryFee = 1;

    public String categoryOf(String couponType) {
        if (couponType == null) {
            return "MAIN";
        }
        return categories.getOrDefault(couponType, "MAIN");
    }
}
