package com.cozy.mall.service.strategy;

import com.cozy.mall.entity.UserCoupon;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
public class BogoCouponStrategy implements CouponStrategy {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String supportedType() { return "BOGO"; }

    @Override
    public BigDecimal calculateDiscount(UserCoupon coupon, BigDecimal orderAmount, List<BigDecimal> drinkPrices) {
        if (drinkPrices == null || drinkPrices.size() < 2) return BigDecimal.ZERO;
        try {
            JsonNode rule = MAPPER.readTree(coupon.getRuleJson());
            BigDecimal maxPerCup = rule.has("maxPerCup")
                    ? new BigDecimal(rule.get("maxPerCup").asText()) : BigDecimal.valueOf(40);
            return Collections.min(drinkPrices).min(maxPerCup);
        } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
