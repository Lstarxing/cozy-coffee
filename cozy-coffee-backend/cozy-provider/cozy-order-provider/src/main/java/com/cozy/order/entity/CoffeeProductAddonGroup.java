package com.cozy.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品-加料组（规则层，组选择约束）
 * selection_mode=SINGLE/MULTI；min_select/max_select 表达必选 / 可选（MILK 必选 1 项）
 */
@Data
@TableName("coffee_product_addon_group")
public class CoffeeProductAddonGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId; // coffee_products.id
    private String category; // 组类别：MILK/SHOT/SYRUP/OTHER
    private String selectionMode; // SINGLE=单选 / MULTI=多选
    private Integer minSelect; // 最少选择数（MILK=1）
    private Integer maxSelect; // 最多选择数（MILK=1 / SHOT=1）
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
