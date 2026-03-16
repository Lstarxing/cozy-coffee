package com.cozy.member.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AvailableCouponRequest {
    private BigDecimal orderAmount;
    private List<ItemCheckDTO> items;
}
