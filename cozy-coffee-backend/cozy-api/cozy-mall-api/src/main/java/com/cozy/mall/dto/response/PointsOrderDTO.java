package com.cozy.mall.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PointsOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 订单基础信息
    private Long id;
    private String orderNo;
    private Long userId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    // 商品信息
    private Long productId;
    private String productName;
    private String productImage;
    private Integer pointsCost;
    private Integer quantity;

    // 商品类型与交付方式
    private String productType; // VIRTUAL/PHYSICAL
    private String fulfillmentType; // VIRTUAL/PICKUP/DELIVERY

    // 备注
    private String remark;

    // 用户信息（仅详情接口返回）
    private String username;
    private String nickname;
    private String phoneMasked;

    // 交付详情（从 PointsOrderFulfillment 填充）
    private Long storeId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String shippingCompany;
    private String trackingNumber;
    private LocalDateTime shippedAt;
    private String virtualCode;
    private LocalDateTime issuedAt;
    private String pickupCode;
}
