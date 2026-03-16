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
    private String productType; // VIRTUAL/PHYSICAL
    
    // 优惠券配置字段（仅当 category='coupon' 时使用）
    private String couponType; // EXCHANGE/DISCOUNT/FULL_REDUCE/SHOT/BOGO/DELIVERY_FEE 等
    private Integer couponValue; // 折扣率(85=8.5折) 或满减/减多少
    private Integer faceValue; // 兑换券抵扣面值(元)
    private Integer minOrderAmount; // 满减券门槛金额(满多少)
    private Long linkedProductId; // 兑换券关联的咖啡商品ID

    // v4.2 新增
    private Integer monthlyLimit; // 月度兑换限制
    private Integer validDays; // 券有效期（天数）

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
