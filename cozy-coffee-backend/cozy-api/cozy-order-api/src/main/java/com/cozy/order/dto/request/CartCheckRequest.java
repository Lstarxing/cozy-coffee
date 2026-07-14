package com.cozy.order.dto.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CartCheckRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<OrderItemRequest> items;
    private String couponCode;
    private List<String> addonCouponCodes;
    private Long storeId;
    private String pickupTime;
}
