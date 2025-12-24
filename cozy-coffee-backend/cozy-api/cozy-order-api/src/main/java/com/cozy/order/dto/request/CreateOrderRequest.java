package com.cozy.order.dto.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class CreateOrderRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long productId;
    private Integer quantity = 1;
    private String remark;
}
