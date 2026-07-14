package com.cozy.order.api;

import com.cozy.order.dto.request.CartCheckRequest;
import com.cozy.order.dto.request.OrderItemRequest;
import com.cozy.order.dto.response.CheckoutPreviewDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderApiContractTest {

    @Test
    void cartCheckRequestExposesCheckoutInputs() {
        OrderItemRequest item = new OrderItemRequest();
        CartCheckRequest request = new CartCheckRequest();

        request.setItems(List.of(item));
        request.setCouponCode("WELCOME10");
        request.setStoreId(42L);

        assertEquals(List.of(item), request.getItems());
        assertEquals("WELCOME10", request.getCouponCode());
        assertEquals(42L, request.getStoreId());
    }

    @Test
    void checkoutPreviewExposesPreviewToken() {
        CheckoutPreviewDTO preview = new CheckoutPreviewDTO();

        preview.setPreviewToken("preview-token");

        assertEquals("preview-token", preview.getPreviewToken());
    }
}
