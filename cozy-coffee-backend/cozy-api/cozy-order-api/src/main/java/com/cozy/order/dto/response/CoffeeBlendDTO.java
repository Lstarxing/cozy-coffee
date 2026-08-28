package com.cozy.order.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 拼配豆档案（Blend）管理端 CRUD，请求/响应共用。
 * composition 保存为 composition_json：[{"beanId":1,"ratio":60},...]，Σratio=100。
 */
@Data
public class CoffeeBlendDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String code;            // COZY_HOUSE / VELVET_MILK
    private String name;
    private String nameEn;
    private String description;
    private List<BlendCompositionItem> composition;
    private String roast;
    private String flavorNotes;
    private String body;
    private String acidity;
    private Integer sortOrder;
    private String status;          // active/inactive
}
