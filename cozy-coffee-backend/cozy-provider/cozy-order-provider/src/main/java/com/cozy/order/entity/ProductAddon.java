package com.cozy.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * v5.0 加料/配料表
 * 用于定义可选的加料项目（额外浓缩、换燕麦奶等）
 */
@Data
@TableName("product_addons")
public class ProductAddon {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name; // 加料名称：额外浓缩、换燕麦奶
    private String code; // 加料代码：EXTRA_SHOT, OAT_MILK
    private BigDecimal price; // 加料价格
    private String category; // 加料类型：SHOT(浓缩类), MILK(奶类), SYRUP(糖浆类)
    private String description; // 描述
    private String status; // active / inactive
    private Integer sortOrder; // 排序

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
