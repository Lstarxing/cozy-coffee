package com.cozy.mall.service.strategy;

import com.cozy.mall.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券折扣计算策略（Phase 6 策略模式）。
 * 从 PointsMallServiceImpl.calculateCouponDiscount 的 if-else 链抽离。
 * 新增券类型 = 实现本接口 + 注册到 CouponStrategyFactory，不动现有代码。
 */
public interface CouponStrategy {

    /** 券类型标识，匹配 user_coupons.coupon_type */
    String supportedType();

    /**
     * 计算券折扣金额
     * @param coupon 用户持有的券
     * @param orderAmount 订单商品总额
     * @param drinkPrices 饮品单价列表（BOGO 等券需要）
     * @return 折扣金额（正数）
     */
    BigDecimal calculateDiscount(UserCoupon coupon, BigDecimal orderAmount, List<BigDecimal> drinkPrices);
}
