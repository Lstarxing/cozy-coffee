package com.cozy.mall.coupon;

import com.cozy.common.exception.BusinessException;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.entity.UserCoupon;
import com.cozy.mall.util.CouponRuleUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 加浓缩券策略：需订单含「加浓缩」选项，抵扣固定金额（缺省 5）。
 */
@Component("SHOT")
public class ShotCouponCalculator implements CouponCalculator {

    @Override
    public BigDecimal calculate(UserCoupon coupon, BigDecimal orderAmount, List<ItemCheckDTO> items) {
        boolean hasExtraShot = false;
        if (items != null && !items.isEmpty()) {
            for (ItemCheckDTO item : items) {
                String modifiers = item.getModifiersJson();
                if (modifiers != null
                        && (modifiers.contains("\"extraShot\":true")
                                || modifiers.toLowerCase().contains("extra_shot")
                                || modifiers.contains("加浓"))) {
                    hasExtraShot = true;
                    break;
                }
            }
        }

        if (!hasExtraShot) {
            throw new BusinessException("此券仅在点单时选择了【加浓缩】选项后可用");
        }

        int shotValue = CouponRuleUtil.parseValue(coupon.getRuleJson(), "value");
        if (shotValue == 0) {
            shotValue = 5;
        }
        return new BigDecimal(shotValue);
    }
}
