package com.cozy.order.dto.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Admin 加料组保存请求（P2-2）：替换商品全部加料组 + 组内项。
 */
@Data
public class AddonGroupRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String category; // MILK/SHOT/SYRUP/OTHER
    private String selectionMode; // SINGLE/MULTI
    private Integer minSelect;
    private Integer maxSelect;
    private Integer sortOrder;
    private List<AddonItemRequest> items;
}
