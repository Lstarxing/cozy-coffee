package com.cozy.order.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CartCheckResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Long> changedItems;
    private List<Long> invalidItems;
    private CheckoutPreviewDTO preview;
}
