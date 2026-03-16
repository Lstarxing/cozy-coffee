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
    private String coffeeStrength;
    private String optionsJson;

    // 商品图片
    private String productImage;
}
