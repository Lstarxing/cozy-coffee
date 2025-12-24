package com.cozy.member.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PointsOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer pointsCost;
    private Integer quantity;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String status;
    private LocalDateTime createdAt;

    // 物流信息
    private String shippingCompany;
    private String trackingNumber;
    private LocalDateTime shippedAt;

    // 配送方式
    private String deliveryType; // express/pickup
}
