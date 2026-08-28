package com.cozy.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 咖啡拼配豆（Blend）
 * composition_json 保存拼配比例：[{"beanId":1,"ratio":60},{"beanId":2,"ratio":40}]，合计 100。
 */
@Data
@TableName("coffee_blend")
public class CoffeeBlend {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code; // 拼配代码：COZY_HOUSE / VELVET_MILK
    private String name; // 拼配名（中文）
    private String nameEn; // 拼配名（英文）
    private String description; // 拼配简介
    private String compositionJson; // 拼配比例 JSON
    private String roast; // 拼配整体烘焙度
    private String flavorNotes; // 拼配整体风味
    private String body; // 醇厚度
    private String acidity; // 酸度
    private Integer sortOrder;
    private String status; // active/inactive

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
