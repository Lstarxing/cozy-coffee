package com.cozy.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项实体（一单多商品）
 */
@Data
@TableName("shop_order_items")
public class ShopOrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal itemAmount;

    // 定制参数
    private String cupSize;
    private String sugarLevel;
    private String temperature;
    private String brewMethod; // 出品方式：POUR_OVER/COLD_BREW（精品 Bean）
    private String coffeeStrength;
    private String optionsJson;

    // v5.0: 加料信息
    private String addonsJson; // JSON格式存储加料: [{"id":1,"name":"额外浓缩","price":5}]
    private BigDecimal addonsAmount; // 加料总金额

    private LocalDateTime createdAt;
}
