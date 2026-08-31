package com.cozy.order.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

/**
 * 订单项请求
 */
@Data
public class OrderItemRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "购买数量至少为1")
    @Max(value = 10, message = "单商品购买数量不能超过10")
    private Integer quantity = 1;

    private String cupSize;
    private String sugarLevel;
    private String temperature;
    private String brewMethod; // 出品方式：POUR_OVER/COLD_BREW（精品 Bean 必选）
    private String coffeeStrength;
    private String optionsJson;

    private String addonsJson;
}
