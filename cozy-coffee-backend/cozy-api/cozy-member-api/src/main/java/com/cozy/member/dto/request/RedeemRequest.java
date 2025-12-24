package com.cozy.member.dto.request;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class RedeemRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "地址ID不能为空")
    private Long addressId;

    @Min(value = 1, message = "数量至少为1")
    private Integer quantity = 1;
}
