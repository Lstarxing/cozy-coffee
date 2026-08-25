package com.cozy.order.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    /** 优惠券抵扣明细（优惠详情展开用）；无券时为空列表 */
    private List<CouponPreviewItem> couponDetails = new ArrayList<>();

    @Data
    public static class CouponPreviewItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String title;
        private BigDecimal discount;
        private boolean main;
    }
}
