package com.cozy.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品-加料明细（组内项 + 默认标记 + price_delta）
 * price_delta = 该商品选择该 Addon 的实际价格增量，默认项恒 0，禁止负值
 */
@Data
@TableName("coffee_product_addon")
public class CoffeeProductAddon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId; // coffee_product_addon_group.id
    private Long addonId; // product_addons.id
    private Boolean isDefault; // 组内默认项（如全脂奶 / 标准）
    private BigDecimal priceDelta; // 该商品选择该 Addon 的实际价格增量；默认项恒 0，禁止负值
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
