package com.cozy.order.service;

import com.cozy.order.dto.request.CartCheckRequest;
import com.cozy.order.dto.request.OrderItemRequest;
import com.cozy.order.dto.response.CartCheckResultDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderPreviewServiceTest {

    private CoffeeProductMapper productMapper;
    private OrderPreviewService service;

    @BeforeEach
    void setUp() {
        productMapper = mock(CoffeeProductMapper.class);
        service = new OrderPreviewService(productMapper, new ProductSkuValidationService(), new ObjectMapper());
    }

    @Test
    void calculatesLargeSurchargeWithoutPersisting() {
        when(productMapper.selectById(10L)).thenReturn(product(10L, "active", "20.00"));

        CartCheckResultDTO result = service.preview(7L, "basic", request(item(10L, 2, "LARGE")));

        assertEquals(new BigDecimal("46.00"), result.getPreview().getSubtotal());
        assertEquals(new BigDecimal("46.00"), result.getPreview().getPayable());
        assertTrue(result.getInvalidItems().isEmpty());
        assertNotNull(result.getPreview().getPreviewToken());
    }

    @Test
    void reportsOfflineItem() {
        when(productMapper.selectById(11L)).thenReturn(product(11L, "inactive", "20.00"));

        CartCheckResultDTO result = service.preview(7L, "basic", request(item(11L, 1, "STANDARD")));

        assertEquals(List.of(11L), result.getInvalidItems());
        assertEquals(new BigDecimal("0.00"), result.getPreview().getPayable());
    }

    @Test
    void tokenIsStableAndChangesWithQuantity() {
        when(productMapper.selectById(12L)).thenReturn(product(12L, "active", "18.00"));
        CartCheckRequest first = request(item(12L, 1, "STANDARD"));
        CartCheckRequest same = request(item(12L, 1, "STANDARD"));
        CartCheckRequest changed = request(item(12L, 2, "STANDARD"));

        String firstToken = service.preview(7L, "basic", first).getPreview().getPreviewToken();
        assertEquals(firstToken, service.preview(7L, "basic", same).getPreview().getPreviewToken());
        assertNotEquals(firstToken, service.preview(7L, "basic", changed).getPreview().getPreviewToken());
    }

    private CartCheckRequest request(OrderItemRequest item) {
        CartCheckRequest request = new CartCheckRequest();
        request.setItems(List.of(item));
        request.setStoreId(1L);
        return request;
    }

    private OrderItemRequest item(Long productId, int quantity, String size) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setCupSize(size);
        item.setSugarLevel("STANDARD");
        item.setTemperature("HOT");
        return item;
    }

    private CoffeeProduct product(Long id, String status, String price) {
        CoffeeProduct product = new CoffeeProduct();
        product.setId(id);
        product.setName("Coffee " + id);
        product.setStatus(status);
        product.setPrice(new BigDecimal(price));
        product.setCategory("coffee");
        product.setSizeType("MEDIUM_LARGE");
        product.setSugarType("FREE_CHOICE");
        product.setTempType("ALL_OK");
        product.setUpdatedAt(LocalDateTime.of(2026, 7, 14, 12, 0));
        return product;
    }
}
