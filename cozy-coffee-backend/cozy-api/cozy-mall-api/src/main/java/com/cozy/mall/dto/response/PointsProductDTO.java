package com.cozy.mall.dto.response;

import lombok.Data;
import java.io.Serializable;

@Data
public class PointsProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Integer pointsPrice;
    private Integer stock;
    private String category;
}
