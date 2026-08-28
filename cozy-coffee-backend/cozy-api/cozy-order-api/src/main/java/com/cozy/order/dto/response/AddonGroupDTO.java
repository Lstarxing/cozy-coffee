package com.cozy.order.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 商品加料组（菜单 / 详情 API 返回，前端渲染规格选项）。
 * 组选择约束 + 组内项（含 price_delta 权威价、默认项标记）。
 */
@Data
public class AddonGroupDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String category; // MILK / SHOT / SYRUP / OTHER
    private String selectionMode; // SINGLE / MULTI
    private Integer minSelect;
    private Integer maxSelect;
    private Integer sortOrder;
    private List<AddonItemDTO> items;
}
