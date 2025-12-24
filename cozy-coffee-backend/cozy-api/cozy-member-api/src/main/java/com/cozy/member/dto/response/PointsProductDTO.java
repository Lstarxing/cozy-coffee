package com.cozy.member.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PointsProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Integer pointsPrice;
    private BigDecimal originalPrice;
    private Integer stock;
    private String status;
    private String category;
}
