package com.cozy.mall.coupon;

import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券抵扣策略接口（用券侧，按 coupon_type 分发）。
 * 实现类以 @Component(券类型) 注册，Spring 注入 Map&lt;type, calculator&gt; 供 calculateCouponDiscount 分发。
 */
public interface CouponCalculator {

    /**
     * 计算券抵扣金额
     *
     * @param coupon      用户券实例（含 ruleJson 规则快照）
     * @param orderAmount 订单金额
     * @param items       订单商品明细
     * @return 抵扣金额；不适用/未知返回 ZERO
     */
    BigDecimal calculate(UserCoupon coupon, BigDecimal orderAmount, List<ItemCheckDTO> items);
}
