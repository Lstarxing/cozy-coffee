package com.cozy.member.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AvailableCouponRequest {
    @NotNull(message = "订单金额不能为空")
    private BigDecimal orderAmount;

    @Valid
    private List<ItemCheckDTO> items;
}
