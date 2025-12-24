package com.cozy.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("points_products")
public class PointsProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Integer pointsPrice;
    private BigDecimal originalPrice;
    private Integer stock;
    private String status;
    private String category;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
