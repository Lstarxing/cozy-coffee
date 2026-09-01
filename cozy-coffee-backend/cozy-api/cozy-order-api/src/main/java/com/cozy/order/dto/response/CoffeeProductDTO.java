package com.cozy.order.dto.response;

import lombok.Data;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CoffeeProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String productCode; // 商品短码（3.5 资产命名，身份标识）
    private String description;
    @Size(max = 40, message = "列表简短描述最多 40 个字符")
    private String shortDescription; // 菜单列表凝练一句（≤40 字，完整展示不省略）；选规格/详情用 description
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

    // V2：标签（展示用；NEW/COLD/FRUITY...，TOP1 数据驱动不静态录入）
    private List<String> tags;

    // V2：出品方式（精品 Bean 必选规格）：POUR_OVER/COLD_BREW；NULL=非 Bean 商品
    private String brewMethod;
    private BigDecimal coldBrewPrice;

    // V2（P2）：加料组（菜单/详情渲染规格选项）
    private List<AddonGroupDTO> addonGroups;

    // V2（P2 收尾）：豆/拼配档案（菜单/详情读侧；bean_id / blend_id 二选一）
    private Long beanId;
    private Long blendId;
    private BeanProfileDTO beanProfile;
    private BlendProfileDTO blendProfile;

    // 规格允许选项（单一事实源，后端由 sizeType/sugarType/tempType 规范值计算；前端渲染用）
    private List<String> allowedSizes;
    private List<String> allowedSugars;
    private List<String> allowedTemps;
}
