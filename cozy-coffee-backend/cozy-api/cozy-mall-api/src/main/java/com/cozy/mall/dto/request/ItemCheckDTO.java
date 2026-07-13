package com.cozy.mall.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCheckDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    private BigDecimal price;

    private String category;

    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    private String modifiersJson;

    private String cupSize;

    private Boolean isNewProduct;
}
