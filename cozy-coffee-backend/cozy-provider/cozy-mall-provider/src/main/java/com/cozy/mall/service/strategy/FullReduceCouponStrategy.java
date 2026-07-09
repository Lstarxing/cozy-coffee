package com.cozy.mall.service.strategy;

import com.cozy.mall.entity.UserCoupon;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FullReduceCouponStrategy implements CouponStrategy {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String supportedType() { return "FULL_REDUCE"; }

    @Override
    public BigDecimal calculateDiscount(UserCoupon coupon, BigDecimal orderAmount, List<BigDecimal> drinkPrices) {
        try {
            JsonNode rule = MAPPER.readTree(coupon.getRuleJson());
            BigDecimal threshold = new BigDecimal(rule.get("threshold").asText());
            if (orderAmount.compareTo(threshold) < 0) return BigDecimal.ZERO;
            return new BigDecimal(rule.get("reduceAmount").asText());
        } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
