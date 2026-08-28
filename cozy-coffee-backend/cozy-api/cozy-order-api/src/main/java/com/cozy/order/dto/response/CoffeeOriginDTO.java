package com.cozy.order.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 咖啡来源主题档案（Origin Archive）管理端 CRUD，请求/响应共用。
 */
@Data
public class CoffeeOriginDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String code;            // ETHIOPIA / KENYA ...
    private String country;         // Ethiopia
    private String countryZh;       // 埃塞俄比亚
    private String region;          // Yirgacheffe / Sidamo
    private String typicalCharacter; // 典型气质（探索页文案）
    private String description;     // 来源故事
    private Integer sortOrder;
    private String status;          // active/inactive
}
