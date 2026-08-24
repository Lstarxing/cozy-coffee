package com.cozy.mall.coupon;

import com.cozy.common.exception.BusinessException;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.entity.UserCoupon;
import com.cozy.mall.util.CouponRuleUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 买一送一券策略：低价免单（需至少 2 杯饮品），封顶 maxDiscount（缺省 40）。
 */
@Slf4j
@Component("BOGO")
public class BogoCouponCalculator implements CouponCalculator {

    @Override
    public BigDecimal calculate(UserCoupon coupon, BigDecimal orderAmount, List<ItemCheckDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("无法获取商品信息");
        }

        List<BigDecimal> drinkPrices = new ArrayList<>();
        for (ItemCheckDTO item : items) {
            if (CouponRuleUtil.isDrink(item.getCategory())) {
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                for (int i = 0; i < qty; i++) {
                    drinkPrices.add(item.getPrice());
                }
            }
        }

        if (drinkPrices.size() < 2) {
            throw new BusinessException("买一送一券需要至少2杯饮品");
        }

        Collections.sort(drinkPrices); // 升序
        int maxDiscountFromRule = CouponRuleUtil.parseValue(coupon.getRuleJson(), "maxDiscount");
        BigDecimal maxPerCup = maxDiscountFromRule > 0 ? new BigDecimal(maxDiscountFromRule) : new BigDecimal("40");
        BigDecimal cheapestPrice = drinkPrices.get(0);
        BigDecimal discount = cheapestPrice.min(maxPerCup);

        log.info("BOGO券抵扣: 最便宜饮品={}，封顶={}, 实际抵扣={}", cheapestPrice, maxPerCup, discount);
        return discount;
    }
}
