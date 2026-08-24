package com.cozy.mall.coupon;

import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.entity.UserCoupon;
import com.cozy.mall.util.CouponRuleUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 配送费抵扣券策略：抵扣固定金额（缺省 3）。
 */
@Component("DELIVERY_FEE")
public class DeliveryFeeCouponCalculator implements CouponCalculator {

    @Override
    public BigDecimal calculate(UserCoupon coupon, BigDecimal orderAmount, List<ItemCheckDTO> items) {
        int maxFeeDiscount = CouponRuleUtil.parseValue(coupon.getRuleJson(), "value");
        if (maxFeeDiscount == 0) {
            maxFeeDiscount = 3;
        }
        return new BigDecimal(maxFeeDiscount);
    }
}
