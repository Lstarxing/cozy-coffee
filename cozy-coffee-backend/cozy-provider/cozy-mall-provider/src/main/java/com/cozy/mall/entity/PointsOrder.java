package com.cozy.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("points_orders")
public class PointsOrder {
    @TableId(type = IdType.AUTO)
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
    private String shippingCompany;
    private String trackingNumber;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
