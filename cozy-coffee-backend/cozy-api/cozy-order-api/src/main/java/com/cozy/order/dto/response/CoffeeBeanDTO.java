package com.cozy.order.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 单品豆档案（Bean Profile）管理端 CRUD，请求/响应共用。
 */
@Data
public class CoffeeBeanDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String code;            // ETH_WASHED_LIGHT
    private String name;
    private String nameEn;
    private Long originId;          // coffee_origin.id（逻辑关联）
    private String originName;      // 响应补产区名（Admin 展示用）
    private String altitude;
    private String processing;
    private String variety;
    private String roast;
    private String flavorNotes;
    private String body;
    private String acidity;
    private String role;            // 浓缩基底 / 花香层次
    private String description;
    private Integer sortOrder;
    private String status;          // active/inactive
}
