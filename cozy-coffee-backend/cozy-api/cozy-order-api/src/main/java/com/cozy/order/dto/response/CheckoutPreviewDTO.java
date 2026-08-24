package com.cozy.order.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CheckoutPreviewDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal payable;
    private Integer pointsEarned;
    private Integer expEarned;
    private String previewToken;
    private LocalDateTime expiresAt;
}
