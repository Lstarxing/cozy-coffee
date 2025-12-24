package com.cozy.order.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ShopOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String orderNo;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Integer pointsEarned;
    private BigDecimal pointsMultiplier;
    private String status;
    private LocalDateTime createdAt;

    // 取餐码相关
    private String pickupCode;
    private LocalDate businessDate;
}
