package com.cozy.mall.service.strategy;

import com.cozy.mall.entity.UserCoupon;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class DiscountCouponStrategy implements CouponStrategy {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String supportedType() { return "DISCOUNT"; }

    @Override
    public BigDecimal calculateDiscount(UserCoupon coupon, BigDecimal orderAmount, List<BigDecimal> drinkPrices) {
        try {
            JsonNode rule = MAPPER.readTree(coupon.getRuleJson());
            BigDecimal rate = new BigDecimal(rule.get("discountRate").asText());
            BigDecimal maxDiscount = rule.has("maxDiscount")
                    ? new BigDecimal(rule.get("maxDiscount").asText()) : BigDecimal.valueOf(9999);
            return orderAmount.multiply(BigDecimal.ONE.subtract(rate))
                    .setScale(0, RoundingMode.HALF_UP).min(maxDiscount);
        } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
