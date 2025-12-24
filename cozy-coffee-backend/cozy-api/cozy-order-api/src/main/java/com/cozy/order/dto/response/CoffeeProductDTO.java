package com.cozy.order.dto.response;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CoffeeProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String category;
}
