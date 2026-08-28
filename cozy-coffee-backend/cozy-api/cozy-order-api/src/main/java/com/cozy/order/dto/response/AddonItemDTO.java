package com.cozy.order.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品加料组内项（价格 = coffee_product_addon.price_delta 权威增量，前端只展示不推导）。
 */
@Data
public class AddonItemDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long addonId; // product_addons.id
    private String code;
    private String name;
    private BigDecimal priceDelta;
    private Boolean isDefault;
    private Integer sortOrder;
}
