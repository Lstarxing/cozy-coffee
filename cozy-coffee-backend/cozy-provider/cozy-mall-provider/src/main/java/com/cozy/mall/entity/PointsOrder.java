package com.cozy.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
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
    private String status;
    private LocalDateTime completedAt;
    private String remark;

    // 商品类型与交付方式
    private String productType; // VIRTUAL/PHYSICAL
    private String fulfillmentType; // VIRTUAL/PICKUP/DELIVERY
    private LocalDate businessDate; // 业务日期

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
