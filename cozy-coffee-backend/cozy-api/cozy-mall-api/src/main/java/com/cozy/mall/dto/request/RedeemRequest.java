package com.cozy.mall.dto.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class RedeemRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long productId;
    private Integer quantity = 1;
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
}
