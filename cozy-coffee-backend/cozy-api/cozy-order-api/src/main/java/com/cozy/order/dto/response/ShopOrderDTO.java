package com.cozy.order.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 订单 DTO（支持多商品）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** True when an earlier request with the same idempotency key created this order. */
    private Boolean idempotentReplay;

    // 订单基础信息
    private Long id;
    private String orderNo;
    private Long userId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long storeId;
    private LocalDate businessDate;

    // 用户信息（仅详情接口返回）
    private String username;
    private String nickname;
    private String phoneMasked;
    private String memberLevel; // v5.0: 会员等级 (basic, silver, gold, diamond, black)

    // 取餐信息
    private String pickupCode;
    private LocalDateTime pickupCodeGeneratedAt;
    private String diningMethod;

    // 金额信息
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private Integer totalQuantity;

    // 券信息
    private Long appliedCouponId;

    // 商品摘要（列表展示用，如"美式x2,拿铁x1"）
    private String itemsSummary;

    // 商品明细（详情接口返回）
    private List<ShopOrderItemDTO> items;

    // 备注
    private String remark;

    // 积分信息
    private Integer expEarned;
    private Integer pointsEarned;
    private BigDecimal pointsMultiplier;
    private Boolean rewardsGranted;

    // v5.3: 配送费相关
    private BigDecimal deliveryFee; // 原始配送费金额
    private Boolean deliveryFeeWaived; // 配送费是否已减免
    private String deliveryFeeWaivedReason; // 减免原因 (BLACK_GOLD_UNLIMITED / COUPON)

    // 待处理订单超时信息
    private LocalDateTime expireAt; // 预计自动取消时间
    private Long secondsToExpire; // 当前剩余秒数（最小为0）
    private Boolean aboutToExpire; // 是否即将过期（<=30秒）

    // v6.2: MQ 解耦用 —— OrderServiceImpl.completeOrder 内计算，controller 组装事件发给 MQ
    private Boolean isFirstOrder; // 本单是否为用户首单
    private Boolean hasNewProduct; // 订单项中是否包含新品商品

}
