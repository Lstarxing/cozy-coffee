package com.cozy.mall.coupon;

import com.cozy.common.exception.BusinessException;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.entity.UserCoupon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 新品半价券策略：自动选择价格最高的新品，半价封顶 20。
 */
@Component("NEW_PRODUCT_HALF")
public class NewProductHalfCouponCalculator implements CouponCalculator {

    @Override
    public BigDecimal calculate(UserCoupon coupon, BigDecimal orderAmount, List<ItemCheckDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("此券仅适用于新品饮品，请先添加新品商品");
        }

        ItemCheckDTO highestNewProduct = null;
        BigDecimal highestPrice = BigDecimal.ZERO;
        for (ItemCheckDTO item : items) {
            if (Boolean.TRUE.equals(item.getIsNewProduct())) {
                BigDecimal itemPrice = item.getPrice();
                if (highestNewProduct == null || itemPrice.compareTo(highestPrice) > 0) {
                    highestNewProduct = item;
                    highestPrice = itemPrice;
                }
            }
        }

        if (highestNewProduct == null) {
            throw new BusinessException("此券仅限新品饮品使用，当前订单中没有新品商品");
        }

        BigDecimal halfPrice = highestPrice.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal maxDiscount = new BigDecimal("20");
        return halfPrice.compareTo(maxDiscount) > 0 ? maxDiscount : halfPrice;
    }
}
