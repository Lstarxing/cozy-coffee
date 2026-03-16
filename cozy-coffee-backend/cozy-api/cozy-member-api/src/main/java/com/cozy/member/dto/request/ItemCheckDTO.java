package com.cozy.member.dto.request;

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

    private Long productId;
    private BigDecimal price; // 单价
    private String category; // 类目
    private Integer quantity; // 数量

    // v5.3: 修饰符信息 (用于 SHOT 券校验)
    private String modifiersJson; // JSON: {"extraShot": true, "milkType": "oat"}
    
    // v5.3: 杯型信息 (用于优惠券 SKU 限制校验，如“仅限标准杯”)
    private String cupSize; // STANDARD/LARGE    
    // v5.3: 新品标识 (用于新品券校验)
    private Boolean isNewProduct;}
