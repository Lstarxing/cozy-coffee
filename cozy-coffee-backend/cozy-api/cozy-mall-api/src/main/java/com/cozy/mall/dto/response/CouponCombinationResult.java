package com.cozy.mall.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 优惠券组合核销/预览结果。
 * <p>
 * 由 CouponCombinationService 统一产出（主券/辅券分类、组合校验、金额计算），
 * order 层只消费结果，不再自行理解"谁是主券/谁能叠加"。
 * mainDiscount 已含尊享通兑券的 freeAddon 免加料部分。
 */
@Data
public class CouponCombinationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主券折扣（含 freeAddon 免加料） */
    private BigDecimal mainDiscount = BigDecimal.ZERO;

    /** 辅券商品抵扣（如 SHOT 加浓缩券） */
    private BigDecimal addonDiscount = BigDecimal.ZERO;

    /** 配送费减免（DELIVERY_FEE 辅券） */
    private BigDecimal deliveryFeeDiscount = BigDecimal.ZERO;

    /** 主券 ID（无主券时为 null） */
    private Long mainCouponId;

    /** 辅券 ID 列表 */
    private List<Long> addonCouponIds = new ArrayList<>();

    /** 主券类型（无主券时为 null） */
    private String mainCouponType;

    /** 是否使用兑换券（使用兑换券不发放积分） */
    private boolean exchangeCoupon;

    /** 是否互斥券（不可与其他券叠加） */
    private boolean exclusive;
}
