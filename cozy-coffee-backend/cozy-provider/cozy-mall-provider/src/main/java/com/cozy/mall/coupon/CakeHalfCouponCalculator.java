package com.cozy.mall.coupon;

import com.cozy.common.exception.BusinessException;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.entity.UserCoupon;
import com.cozy.mall.util.CouponRuleUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 蛋糕 5 折券策略：限烘培甜品，自动选择最高价商品，5 折封顶 50。
 * 兼容 CAKE_HALF 类型与 ruleJson 含 CAKE_ONLY scope 的 EXCHANGE 券。
 */
@Component("CAKE_HALF")
public class CakeHalfCouponCalculator implements CouponCalculator {

    @Override
    public BigDecimal calculate(UserCoupon coupon, BigDecimal orderAmount, List<ItemCheckDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("此券仅适用于烘培甜品，请先添加烘培商品");
        }

        ItemCheckDTO highestBakeryProduct = null;
        BigDecimal highestPrice = BigDecimal.ZERO;
        for (ItemCheckDTO item : items) {
            if (CouponRuleUtil.isBakery(item.getCategory())) {
                BigDecimal itemPrice = item.getPrice();
                if (highestBakeryProduct == null || itemPrice.compareTo(highestPrice) > 0) {
                    highestBakeryProduct = item;
                    highestPrice = itemPrice;
                }
            }
        }

        if (highestBakeryProduct == null) {
            throw new BusinessException("此券仅限烘培甜品使用，当前订单中没有烘培商品");
        }

        BigDecimal halfPrice = highestPrice.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal maxDiscount = new BigDecimal("50");
        return halfPrice.compareTo(maxDiscount) > 0 ? maxDiscount : halfPrice;
    }
}
