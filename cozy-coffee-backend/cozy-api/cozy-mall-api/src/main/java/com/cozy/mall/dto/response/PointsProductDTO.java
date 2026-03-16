package com.cozy.mall.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PointsProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;
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
    private String couponType; // EXCHANGE/DISCOUNT/FULL_REDUCE
    private Integer couponValue; // 折扣率(85=8.5折) 或满减/减多少
    private Integer faceValue; // 兑换券抵扣面值(元) - 保留兼容
    private Integer minOrderAmount; // 满减券门槛金额(满多少)
    private Long linkedProductId; // 兑换券关联的咖啡商品ID
    private String linkedProductName; // 兑换券关联的咖啡商品名称(展示用)
}
