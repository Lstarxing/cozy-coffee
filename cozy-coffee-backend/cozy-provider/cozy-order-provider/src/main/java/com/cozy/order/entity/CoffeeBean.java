package com.cozy.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 单品豆档案（Bean Profile）
 * 定位为豆档案，不宣称批次（字段无 batch/lot/roast_date）；如需库存/采购批次管理，未来单独建 Bean Batch 表。
 */
@Data
@TableName("coffee_bean")
public class CoffeeBean {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code; // 豆档案代码：ETH_WASHED_LIGHT
    private String name; // 豆名（中文）
    private String nameEn; // 豆名（英文）
    private Long originId; // 所属来源主题：coffee_origin.id（逻辑关联）
    private String altitude; // 海拔：1,800-2,200m
    private String processing; // 处理法：Washed / Natural
    private String variety; // 品种：Heirloom / 74110
    private String roast; // 烘焙度：Light / Medium-Dark
    private String flavorNotes; // 风味
    private String body; // 醇厚度：Full / Smooth / Dense
    private String acidity; // 酸度：Balanced / Bright
    private String role; // 角色：浓缩基底 / 花香层次
    private String description; // 豆档案简介
    private Integer sortOrder;
    private String status; // active/inactive

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
