package com.cozy.order.dto.request;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 创建订单请求（支持多商品）
 */
@Data
public class CreateOrderRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    // 多商品订单
    private List<OrderItemRequest> items;

    // 券码（可选）
    private String couponCode;

    // v5.0: 用餐方式 (DINE_IN / TAKEOUT / DELIVERY)
    private String diningMethod;

    // v5.0: 附加券码列表（如配送费抵扣券，可与主券叠加使用）
    private List<String> addonCouponCodes;

    // v5.0: 外卖配送地址ID（当 diningMethod = DELIVERY 时必填）
    private Long deliveryAddressId;

    // 备注
    private String remark;

    // ========== 兼容旧字段（单商品模式，过渡期）==========
    @Deprecated
    private Long productId;
    @Deprecated
    private Integer quantity = 1;
}
