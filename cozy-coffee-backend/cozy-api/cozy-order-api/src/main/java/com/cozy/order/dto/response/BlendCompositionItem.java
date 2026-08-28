package com.cozy.order.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 拼配比例项：coffee_blend.composition_json 的原子单元。
 */
@Data
public class BlendCompositionItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long beanId;
    private Integer ratio;
}
