package com.cozy.mall.coupon;

import com.cozy.common.exception.BusinessException;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.entity.UserCoupon;
import com.cozy.mall.util.CouponRuleUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 折扣券策略：支持 DRINK_ONLY/CAKE_ONLY scope、SINGLE_ITEM 限单件、maxDiscountAmount 封顶。
 * discountPercent 兼容 value 整数与 discountRate 浮点（0.5=5折 / 5=5折 / 8.8=8.8折）。
 */
@Slf4j
@Component("DISCOUNT")
public class DiscountCouponCalculator implements CouponCalculator {

    @Override
    public BigDecimal calculate(UserCoupon coupon, BigDecimal orderAmount, List<ItemCheckDTO> items) {
        String ruleJson = coupon.getRuleJson();

        // 1. 折扣百分比，兼容多种格式
        int discountPercent = CouponRuleUtil.parseValue(ruleJson, "value");
        if (discountPercent <= 0) {
            double floatRate = CouponRuleUtil.parseDoubleValue(ruleJson, "discountRate");
            if (floatRate > 0 && floatRate < 1) {
                discountPercent = (int) (floatRate * 100); // 0.5 -> 50
            } else if (floatRate >= 1 && floatRate <= 10) {
                discountPercent = (int) (floatRate * 10); // 5 -> 50
            } else {
                discountPercent = (int) floatRate;
            }
        }

        if (discountPercent <= 0) {
            log.warn("折扣券无效: discountPercent={}, ruleJson={}", discountPercent, ruleJson);
            return BigDecimal.ZERO;
        }

        // 2. 解析配置（去空格精确匹配）
        String cleanJson = ruleJson != null ? ruleJson.replace(" ", "").replace("\n", "").replace("\t", "") : "";
        boolean isDrinkOnly = cleanJson.contains("\"scope\":\"DRINK_ONLY\"");
        boolean isCakeOnly = cleanJson.contains("\"scope\":\"CAKE_ONLY\"");
        boolean isSingleItem = cleanJson.contains("\"limit\":\"SINGLE_ITEM\"");
        int maxDiscountAmount = CouponRuleUtil.parseValue(ruleJson, "maxDiscountAmount");

        // 3. 确定折扣基数
        BigDecimal baseAmount = orderAmount;

        if (isCakeOnly) {
            BigDecimal maxBakeryPrice = BigDecimal.ZERO;
            if (items != null) {
                for (ItemCheckDTO item : items) {
                    if (CouponRuleUtil.isBakery(item.getCategory())) {
                        if (item.getPrice().compareTo(maxBakeryPrice) > 0) {
                            maxBakeryPrice = item.getPrice();
                        }
                    }
                }
            }
            if (maxBakeryPrice.equals(BigDecimal.ZERO)) {
                throw new BusinessException("此券仅限烘培甜品使用，订单中无烘培商品");
            }
            baseAmount = maxBakeryPrice;
            log.info("蛋糕5折券(CAKE_ONLY): 仅作用于最贵烘培甜品={}", maxBakeryPrice);
        } else if (isDrinkOnly || isSingleItem) {
            BigDecimal maxDrinkPrice = BigDecimal.ZERO;
            BigDecimal drinkTotal = BigDecimal.ZERO;
            if (items != null) {
                for (ItemCheckDTO item : items) {
                    if (CouponRuleUtil.isDrink(item.getCategory())) {
                        int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                        drinkTotal = drinkTotal.add(item.getPrice().multiply(BigDecimal.valueOf(qty)));
                        if (item.getPrice().compareTo(maxDrinkPrice) > 0) {
                            maxDrinkPrice = item.getPrice();
                        }
                    }
                }
            }
            if (maxDrinkPrice.equals(BigDecimal.ZERO)) {
                throw new BusinessException("此券仅限饮品使用，订单中无饮品");
            }
            if (isSingleItem) {
                baseAmount = maxDrinkPrice;
                log.info("折扣券(SINGLE_ITEM): 仅作用于最贵饮品={}", maxDrinkPrice);
            } else {
                baseAmount = drinkTotal;
                log.info("折扣券(DRINK_ONLY): 作用于饮品总额={}", drinkTotal);
            }
        }

        // 4. 计算折扣金额
        BigDecimal rate = new BigDecimal(discountPercent).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        BigDecimal discount = baseAmount.multiply(BigDecimal.ONE.subtract(rate));
        discount = discount.setScale(2, RoundingMode.HALF_UP);

        // 5. 封顶控制
        if (maxDiscountAmount > 0) {
            BigDecimal maxCap = new BigDecimal(maxDiscountAmount);
            if (discount.compareTo(maxCap) > 0) {
                log.info("折扣券封顶: 原折扣={}, 封顶={}", discount, maxCap);
                discount = maxCap;
            }
        }

        return discount;
    }
}
