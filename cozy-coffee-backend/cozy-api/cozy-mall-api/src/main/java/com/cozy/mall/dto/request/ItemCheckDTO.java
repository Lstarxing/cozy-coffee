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

    /**
     * order 权威计算的【当前规格】【每单位】基础价，<b>不含加料费</b>
     * （加料费由 CouponCombinationService 的 addonsTotal / addonPrices 单独传）。
     *
     * <p>由 OrderPreviewer / OrderCreator 在调用 ProductPricingService.price() 之后填入；
     * 是 mall 侧券估值的<b>唯一</b>价格来源 —— mall <b>不得</b>再反查 order 的商品目录取价。
     * 改这里的语义必须同时回归 mall 的券计算，见 docs/adr/0002。
     */
    private BigDecimal price;

    private String category;

    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    private String modifiersJson;

    private String cupSize;

    private Boolean isNewProduct;
}
