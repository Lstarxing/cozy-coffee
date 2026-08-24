package com.cozy.mall.coupon;

import com.cozy.common.exception.BusinessException;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.entity.UserCoupon;
import com.cozy.mall.util.CouponRuleUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 满减券策略：订单金额未满门槛时报错，抵扣固定金额。
 */
@Component("FULL_REDUCE")
public class FullReduceCouponCalculator implements CouponCalculator {

    @Override
    public BigDecimal calculate(UserCoupon coupon, BigDecimal orderAmount, List<ItemCheckDTO> items) {
        String ruleJson = coupon.getRuleJson();
        int minOrderAmount = CouponRuleUtil.parseValue(ruleJson, "minOrderAmount");
        int value = CouponRuleUtil.parseValue(ruleJson, "value");
        if (minOrderAmount > 0 && orderAmount.compareTo(new BigDecimal(minOrderAmount)) < 0) {
            throw new BusinessException("订单金额未满 " + minOrderAmount + " 元");
        }
        return new BigDecimal(value);
    }
}
