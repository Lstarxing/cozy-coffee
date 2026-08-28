package com.cozy.order.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 单品豆档案（菜单/详情读侧）：商品挂 bean_id 时随 CoffeeProductDTO 返回，
 * 移动端选规格页/详情页渲染「烘焙 · 风味 · 醇厚 · 酸度」。
 */
@Data
public class BeanProfileDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String code;
    private String name;
    private String nameEn;
    private String roast;       // 烘焙度
    private String flavorNotes; // 风味
    private String body;        // 醇厚度
    private String acidity;     // 酸度
}
