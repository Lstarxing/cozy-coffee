package com.cozy.order.dto.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Admin 加料组内项保存请求（P2-2）。
 * price_delta = 该商品实际增量；默认项恒 0；禁止负值。
 */
@Data
public class AddonItemRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long addonId; // product_addons.id
    private Boolean isDefault;
    private BigDecimal priceDelta;
    private Integer sortOrder;
}
