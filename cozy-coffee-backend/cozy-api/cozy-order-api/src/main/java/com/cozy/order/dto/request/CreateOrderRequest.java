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

    @Pattern(regexp = "TAKEOUT|DELIVERY", message = "用餐方式不合法")
    private String diningMethod;

    private List<String> addonCouponCodes;

    private Long deliveryAddressId;

    // v6.5: 外送收货人信息快照（移动端确认页携带，后端落库供列表/详情展示）
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    private String remark;

    /** Authoritative token returned by POST /api/order/cart/check. */
    private String previewToken;

    /** Fixed to store 1 in phase one; kept in the contract for future multi-store support. */
    private Long storeId;

    private String pickupTime;

    @Deprecated
    private Long productId;
    @Deprecated
    private Integer quantity = 1;
}
