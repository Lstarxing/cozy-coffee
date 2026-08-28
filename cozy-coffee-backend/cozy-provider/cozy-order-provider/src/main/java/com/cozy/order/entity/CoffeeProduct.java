package com.cozy.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coffee_products")
public class CoffeeProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private BigDecimal price; // 默认价格（中杯）
    private BigDecimal priceMedium; // v5.0: 中杯价格
    private BigDecimal priceLarge; // v5.0: 大杯价格
    private String imageUrl;
    private String category;
    private String status;
    private Integer sortOrder;

    // v5.0: 新品标识
    private Boolean isNewProduct;
    
    // v5.2: SKU 配置字段（已启用）
    private String sizeType;    // 杯型配置：DEFAULT/MEDIUM_LARGE/ALL_SIZES
    private String sugarType;   // 甜度配置：FREE_CHOICE/NO_SUGAR_ONLY/MIN_LESS_SWEET
    private String tempType;    // 温度配置：HOT_COLD/COLD_ONLY/HOT_ONLY（v2 砍温）

    // v2 内容层（Phase 0A）：bean/blend 挂接 + 默认糖度 + 固定组合出杯
    private Long beanId; // 单品豆：coffee_bean.id（与 blendId 互斥）
    private Long blendId; // 拼配豆：coffee_blend.id（与 beanId 互斥）
    private String defaultSugarLevel; // 默认额外加糖等级：STANDARD/LESS/HALF/NO_ADDED_SUGAR；NO_SUGAR_ONLY 商品为 NULL
    private String servingMode; // 出杯模式：NULL=常规 / FIXED_COMBINATION=固定组合（一豆两喝·三喝）
    private String servingConfig; // 固定组合结构化构成 JSON：[{"type":"ESPRESSO","quantity":1},...]
    private String servingDesc; // 固定组合出杯说明（仅展示文案）
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
