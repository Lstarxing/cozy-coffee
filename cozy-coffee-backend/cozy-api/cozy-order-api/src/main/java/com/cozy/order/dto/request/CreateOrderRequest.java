package com.cozy.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 创建订单请求（支持多商品）
 */
@Data
public class CreateOrderRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "请选择商品")
    @Valid
    private List<OrderItemRequest> items;

    private String couponCode;

    @Pattern(regexp = "DINE_IN|TAKEOUT|DELIVERY", message = "用餐方式不合法")
    private String diningMethod;

    private List<String> addonCouponCodes;

    private Long deliveryAddressId;

    private String remark;

    @Deprecated
    private Long productId;
    @Deprecated
    private Integer quantity = 1;
}
