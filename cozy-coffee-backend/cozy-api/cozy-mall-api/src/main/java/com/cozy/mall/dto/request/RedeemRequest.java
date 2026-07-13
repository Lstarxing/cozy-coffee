package com.cozy.mall.dto.request;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 积分兑换请求 v2
 * 支持虚拟商品（无需地址）、自提、快递三种场景
 */
@Data
public class RedeemRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "数量至少为1")
    private Integer quantity = 1;

    /**
     * 交付方式：PICKUP（自提）/ DELIVERY（快递）
     * 虚拟商品可忽略此字段
     */
    private String fulfillmentType;

    /**
     * 旧字段支持：pickup/delivery
     */
    private String deliveryType;

    /**
     * 收货地址ID（DELIVERY 时使用，可选）
     */
    private Long addressId;

    /**
     * 直接传递收货信息（DELIVERY 时使用，与 addressId 二选一）
     */
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    /**
     * 自提门店ID（PICKUP 时使用）
     */
    private Long storeId;

    /**
     * 备注
     */
    private String remark;
}
