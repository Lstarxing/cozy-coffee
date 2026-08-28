package com.cozy.order.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CoffeeProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal priceMedium; // v5.0: 中杯价格
    private BigDecimal priceLarge; // v5.0: 大杯价格
    private String imageUrl;
    private String category;
    private String status; // active/inactive
    private Boolean isNewProduct; // v5.0: 新品标识
    
    // v5.2: SKU 配置字段
    private String sizeType;    // 杯型配置：DEFAULT/MEDIUM_LARGE/ALL_SIZES
    private String sugarType;   // 甜度配置：FREE_CHOICE/NO_SUGAR_ONLY/MIN_LESS_SWEET
    private String tempType;    // 温度配置：HOT_COLD/COLD_ONLY/HOT_ONLY（v2 砍温）
    private String defaultSugarLevel; // 默认额外加糖等级：STANDARD/LESS/HALF/NO_ADDED_SUGAR；NO_SUGAR_ONLY 为 NULL

    // V2：固定组合（一豆两喝/三喝）出杯说明
    private String servingMode; // NULL=常规 / FIXED_COMBINATION=固定组合
    private String servingDesc; // 固定组合出杯说明（仅展示文案）

    // V2（P2）：加料组（菜单/详情渲染规格选项）
    private List<AddonGroupDTO> addonGroups;

    // V2（P2 收尾）：豆/拼配档案（菜单/详情读侧；bean_id / blend_id 二选一）
    private Long beanId;
    private Long blendId;
    private BeanProfileDTO beanProfile;
    private BlendProfileDTO blendProfile;
}
