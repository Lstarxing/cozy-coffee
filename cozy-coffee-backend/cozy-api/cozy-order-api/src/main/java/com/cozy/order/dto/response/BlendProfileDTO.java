package com.cozy.order.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 拼配豆档案（菜单/详情读侧）：商品挂 blend_id 时随 CoffeeProductDTO 返回。
 */
@Data
public class BlendProfileDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String code;
    private String name;
    private String nameEn;
    private String roast;
    private String flavorNotes;
    private String body;
    private String acidity;
    private List<BlendCompositionItem> composition; // 拼配比例
}
