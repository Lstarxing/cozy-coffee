package com.cozy.mall.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PointsOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String orderNo;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private Integer pointsCost;
    private String status;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private LocalDateTime createdAt;
}
