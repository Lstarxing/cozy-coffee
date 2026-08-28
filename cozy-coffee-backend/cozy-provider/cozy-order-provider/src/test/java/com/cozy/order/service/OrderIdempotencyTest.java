package com.cozy.order.service;

import com.cozy.common.exception.BusinessErrorCode;
import com.cozy.common.exception.BusinessException;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.order.entity.ShopOrder;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.mapper.ShopOrderItemMapper;
import com.cozy.order.mapper.ShopOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderIdempotencyTest {

    @Test
    void returnsExistingOrderForSameUserAndKey() {
        ShopOrderMapper orderMapper = mock(ShopOrderMapper.class);
        OrderDtoEnricher enricher = mock(OrderDtoEnricher.class);
        ShopOrder existing = new ShopOrder();
        existing.setId(99L);
        existing.setUserId(7L);
        ShopOrderDTO dto = new ShopOrderDTO();
        dto.setId(99L);
        when(orderMapper.selectByUserAndIdempotencyKey(7L, "key-1")).thenReturn(existing);
        when(enricher.getOrderItemsByOrderId(99L)).thenReturn(List.of());
        when(enricher.toOrderDTO(existing, List.of())).thenReturn(dto);

        OrderCreationService service = service(orderMapper, enricher);
        ShopOrderDTO result = service.createOrder(7L, "basic", "key-1", new CreateOrderRequest());

        assertEquals(99L, result.getId());
        assertTrue(result.getIdempotentReplay());
    }

    @Test
    void rejectsTooLongKey() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service(mock(ShopOrderMapper.class), mock(OrderDtoEnricher.class))
                        .createOrder(7L, "basic", "x".repeat(65), new CreateOrderRequest()));
        assertEquals(BusinessErrorCode.IDEMPOTENCY_KEY_INVALID, error.getCode());
    }

    private OrderCreationService service(ShopOrderMapper orderMapper, OrderDtoEnricher enricher) {
        return new OrderCreationService(
                mock(CoffeeProductMapper.class), orderMapper, mock(ShopOrderItemMapper.class),
                mock(PickupCodeService.class), new ObjectMapper(),
                mock(OrderDtoConverter.class), mock(OrderRewardService.class), enricher,
                mock(OrderInfraService.class), mock(TransactionTemplate.class), mock(OrderPreviewService.class),
                mock(ProductPricingService.class));
    }
}
