package com.cozy.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 兑换订单交付信息表（1:1 关联 points_orders）
 */
@Data
@TableName("points_order_fulfillments")
public class PointsOrderFulfillment {

    @TableId(type = IdType.INPUT)
    private Long orderId;

    /** 交付类型: VIRTUAL/PICKUP/DELIVERY */
    private String type;

    /** 地址簿ID（可选追溯） */
    private Long addressId;

    // 收货快照（DELIVERY 使用）
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    // 物流信息（DELIVERY 使用）
    private String shippingCompany;
    private String trackingNumber;
    private LocalDateTime shippedAt;

    // 自提信息（PICKUP 使用）
    private Long storeId;
    private String pickupCode;
    private LocalDate businessDate;

    // 虚拟发放（VIRTUAL 使用）
    private String virtualCode;
    private LocalDateTime issuedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
