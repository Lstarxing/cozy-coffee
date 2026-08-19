package com.cozy.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单主表实体（多商品支持）
 * 迁移后删除：productId, productName, quantity, unitPrice, cupSize, sugarLevel,
 * temperature, coffeeStrength, optionsJson, pointsMultiplier
 */
@Data
@TableName("shop_orders")
public class ShopOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;
    private String idempotencyKey;
    private BigDecimal totalAmount;

    // 新增字段（v4.0 迁移）
    private Integer totalQuantity;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private Long appliedCouponId;
    private Integer expEarned;
    private Integer pointsEarned;
    private BigDecimal pointsMultiplier;
    private Boolean rewardsGranted;

    private String status;
    private String remark;

    // v5.0: 用餐方式 (TAKEOUT 自提 / DELIVERY 外送)
    private String diningMethod;

    // v5.3: 配送费相关
    private BigDecimal deliveryFee; // 原始配送费金额
    private Boolean deliveryFeeWaived; // 配送费是否已减免（黑金会员自动免运费）
    private String deliveryFeeWaivedReason; // 减免原因 (BLACK_GOLD_UNLIMITED / COUPON / etc.)

    // v6.4: 外送预计送达时间（配送到点自动确认已完成）
    private LocalDateTime expectedDeliveryAt;

    // v5.0: 附加券ID列表 (JSON格式，如 "[1,2,3]"，用于取消时回滚)
    private String appliedAddonCouponIds;

    // 取餐码相关
    private Long storeId;
    private LocalDate businessDate;
    private String pickupCode;
    private LocalDateTime pickupCodeGeneratedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
