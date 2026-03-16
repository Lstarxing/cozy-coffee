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
    private String tempType;    // 温度配置：ALL_OK/COLD_ONLY/HOT_ONLY/NO_HOT
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
