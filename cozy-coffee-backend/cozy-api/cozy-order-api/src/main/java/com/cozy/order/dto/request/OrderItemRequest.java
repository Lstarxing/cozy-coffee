package com.cozy.order.dto.request;

import lombok.Data;
import java.io.Serializable;

/**
 * 订单项请求
 */
@Data
public class OrderItemRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long productId;
    private Integer quantity = 1;

    // 定制参数
    private String cupSize;
    private String sugarLevel;
    private String temperature;
    private String coffeeStrength;
    private String optionsJson;
    
    // v5.3: 加料信息 (JSON格式: [{"code":"EXTRA_SHOT","name":"额外浓缩","price":5}])
    private String addonsJson;
}
