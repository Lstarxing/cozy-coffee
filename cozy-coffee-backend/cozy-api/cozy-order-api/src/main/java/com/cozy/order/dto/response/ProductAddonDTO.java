package com.cozy.order.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 加料主数据（product_addons）目录，Admin 绑定加料组时选择。
 * price = 参考增量（不参与订单计算）。
 */
@Data
public class ProductAddonDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String code;
    private String name;
    private BigDecimal price; // 参考增量
    private String category;
}
