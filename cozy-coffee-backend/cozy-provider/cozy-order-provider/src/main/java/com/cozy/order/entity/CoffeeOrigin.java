package com.cozy.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 咖啡来源主题档案（Origin Archive）
 * 8 产区内容的单一事实来源：web 首页 OriginsJourney / 探索页 / 商品详情豆子档案 + 小程序统一经 API 读取；
 * 地图投影坐标、路线弯度、块级品牌引导语等纯呈现资产留在前端，不进本表。
 */
@Data
@TableName("coffee_origin")
public class CoffeeOrigin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code; // 来源代码：ETHIOPIA/KENYA/BRAZIL/COLOMBIA/GUATEMALA/PANAMA/INDONESIA/YUNNAN
    private String country; // 国家（英文）：Ethiopia
    private String countryZh; // 国家（中文）：埃塞俄比亚
    private String region; // 产区 / 子区域：Yirgacheffe / Sidamo（YUNNAN 只到省级）
    private String typicalCharacter; // 来源地典型气质（一句话，探索页文案）
    private String description; // 来源故事（手冲页 / 探索页文案）
    private Integer sortOrder;
    private String status; // active/inactive

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
