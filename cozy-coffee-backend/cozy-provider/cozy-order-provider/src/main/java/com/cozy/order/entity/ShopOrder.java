package com.cozy.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("shop_orders")
public class ShopOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Integer pointsEarned;
    private BigDecimal pointsMultiplier;
    private String status;
    private String remark;

    // 取餐码相关
    private Long storeId;
    private LocalDate businessDate;
    private String pickupCode;
    private LocalDateTime pickupCodeGeneratedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
