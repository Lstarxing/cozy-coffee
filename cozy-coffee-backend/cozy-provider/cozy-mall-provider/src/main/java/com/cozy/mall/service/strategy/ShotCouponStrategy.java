package com.cozy.mall.service.strategy;

import com.cozy.mall.entity.UserCoupon;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ShotCouponStrategy implements CouponStrategy {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String supportedType() { return "SHOT"; }

    @Override
    public BigDecimal calculateDiscount(UserCoupon coupon, BigDecimal orderAmount, List<BigDecimal> drinkPrices) {
        try {
            JsonNode rule = MAPPER.readTree(coupon.getRuleJson());
            return rule.has("discountValue")
                    ? new BigDecimal(rule.get("discountValue").asText()) : BigDecimal.valueOf(5);
        } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
