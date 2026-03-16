package com.cozy.member.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 券核销结果 DTO
 * 用于下单时返回券类型信息，以便决定是否发放积分
 */
@Data
public class CouponUsageResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 折扣金额 */
    private BigDecimal discountAmount;

    /** 券类型: EXCHANGE/DISCOUNT/FULL_REDUCE */
    private String couponType;

    /** 券ID */
    private Long couponId;

    /** 关联商品ID（仅兑换券有效） */
    private Long linkedProductId;

    /** 是否互斥（不可叠加其他优惠，如积分） */
    private boolean exclusive;
    
    /** v5.7: 免费加料次数（尊享通兑券专属） */
    private int freeAddonCount;

    public CouponUsageResult() {
    }

    public CouponUsageResult(BigDecimal discountAmount, String couponType, Long couponId) {
        this.discountAmount = discountAmount;
        this.couponType = couponType;
        this.couponId = couponId;
    }

    public CouponUsageResult(BigDecimal discountAmount, String couponType, Long couponId, Long linkedProductId,
            boolean exclusive) {
        this.discountAmount = discountAmount;
        this.couponType = couponType;
        this.couponId = couponId;
        this.linkedProductId = linkedProductId;
        this.exclusive = exclusive;
    }
    
    public CouponUsageResult(BigDecimal discountAmount, String couponType, Long couponId, Long linkedProductId,
            boolean exclusive, int freeAddonCount) {
        this.discountAmount = discountAmount;
        this.couponType = couponType;
        this.couponId = couponId;
        this.linkedProductId = linkedProductId;
        this.exclusive = exclusive;
        this.freeAddonCount = freeAddonCount;
    }

    /**
     * 是否为兑换券（使用兑换券不应获得积分）
     */
    public boolean isExchangeCoupon() {
        return "EXCHANGE".equals(couponType);
    }
}
