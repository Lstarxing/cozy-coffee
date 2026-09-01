package com.cozy.order.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 订单项 DTO（一单多商品）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopOrderItemDTO implements Serializable {
    private static final long serialVersionUID = 1L;

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
    private String addonsJson; // 规范化成交加料快照（含默认项，price=price_delta 实际增量）；奶型以这里为准

    // 商品图片
    private String productImage;
}
