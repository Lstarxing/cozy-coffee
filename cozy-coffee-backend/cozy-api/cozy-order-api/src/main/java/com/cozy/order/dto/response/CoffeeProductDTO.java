package com.cozy.order.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CoffeeProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal priceMedium; // v5.0: 中杯价格
    private BigDecimal priceLarge; // v5.0: 大杯价格
    private String imageUrl;
    private String category;
    private String status; // active/inactive
    private Boolean isNewProduct; // v5.0: 新品标识
    
    // v5.2: SKU 配置字段
    private String sizeType;    // 杯型配置：DEFAULT/MEDIUM_LARGE/ALL_SIZES
    private String sugarType;   // 甜度配置：FREE_CHOICE/NO_SUGAR_ONLY/MIN_LESS_SWEET
    private String tempType;    // 温度配置：HOT_COLD/COLD_ONLY/HOT_ONLY（v2 砍温）
}
