package com.cozy.order.service;

import com.cozy.mall.api.PointsMallService;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.mall.dto.response.CouponCombinationResult;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.request.OrderItemRequest;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.mapper.ShopOrderItemMapper;
import com.cozy.order.mapper.ShopOrderMapper;
import com.cozy.order.service.converter.OrderDtoConverter;
import com.cozy.order.service.converter.OrderDtoEnricher;
import com.cozy.order.service.infra.OrderCancelledEventPublisher;
import com.cozy.order.service.infra.OrderTimeoutIndexer;
import com.cozy.order.service.order.OrderCreator;
import com.cozy.order.service.order.OrderPreviewer;
import com.cozy.order.service.order.OrderRewardService;
import com.cozy.order.service.order.PickupCodeService;
import com.cozy.order.service.product.ProductPricingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 下单路径的 ItemCheckDTO.price 契约守卫（见 docs/adr/0002）。
 *
 * <p>预览路径由 {@code OrderPreviewerTest#itemCheckPriceIsUnitBasePriceOfChosenSpec} 守住；
 * OrderCreator 另有一套自己的 ItemCheckDTO 构造逻辑（给实际核销用），所以这里单独钉住，
 * 并**带上加料** —— 证明「price 不含加料费」而不是只证明"等于规格价"。
 *
 * <p>本测试只关心 useCouponCombination 的入参：后面的落库在 TransactionTemplate 回调里，
 * TransactionTemplate 是 mock，回调不执行，正好避免为持久化伪造一大堆桩。
 */
class OrderCreatorItemCheckContractTest {

    private static final long PRODUCT_ID = 13L;
    private static final int QTY = 2;

    private PointsMallService pointsMallService;

    private OrderCreator service;

    @Test
    @SuppressWarnings("unchecked")
    void itemCheckPriceIsUnitBasePriceExcludingAddons() {
        CoffeeProductMapper productMapper = mock(CoffeeProductMapper.class);
        ProductPricingService pricing = mock(ProductPricingService.class);

        CoffeeProduct product = new CoffeeProduct();
        product.setId(PRODUCT_ID);
        product.setName("Coffee 13");
        product.setStatus("active");
        product.setCategory("coffee");
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

        // 大杯基础价 23、加料费 5/件、加料明细 [5] —— 直接给定价结果，绕开真实定价链路
        when(pricing.price(any(), any(), any(), any(), any(), any())).thenReturn(
                new ProductPricingService.PriceResult(true, null,
                        new BigDecimal("23"), new BigDecimal("5"), new BigDecimal("28"),
                        "[]", List.of(new BigDecimal("5"))));

        pointsMallService = mock(PointsMallService.class);
        when(pointsMallService.useCouponCombination(any(), anyList(), any(), any(), anyList(), anyList()))
                .thenReturn(new CouponCombinationResult());

        service = build(productMapper, pricing);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(item()));
        request.setCouponCode("C1");
        request.setDiningMethod("PICKUP");
        request.setStoreId(1L);

        service.createOrder(7L, "basic", null, request);

        ArgumentCaptor<List<ItemCheckDTO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<BigDecimal> addonsTotalCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<List<BigDecimal>> addonPricesCaptor = ArgumentCaptor.forClass(List.class);
        verify(pointsMallService).useCouponCombination(any(), anyList(), any(),
                addonsTotalCaptor.capture(), addonPricesCaptor.capture(), itemsCaptor.capture());

        // 每单位的大杯基础价：2 件仍是 23，不是整行 46
        ItemCheckDTO check = itemsCaptor.getValue().get(0);
        assertEquals(0, new BigDecimal("23").compareTo(check.getPrice()));
        assertEquals(QTY, check.getQuantity());

        // 加料只进 addonsTotal / addonPrices，不混进 price
        assertEquals(0, new BigDecimal("10").compareTo(addonsTotalCaptor.getValue()));
        assertEquals(1, addonPricesCaptor.getValue().size());
        assertEquals(0, new BigDecimal("5").compareTo(addonPricesCaptor.getValue().get(0)));
    }

    private OrderItemRequest item() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(PRODUCT_ID);
        item.setQuantity(QTY);
        item.setCupSize("LARGE");
        item.setSugarLevel("STANDARD");
        item.setTemperature("HOT");
        return item;
    }

    private OrderCreator build(CoffeeProductMapper productMapper, ProductPricingService pricing) {
        OrderDtoEnricher enricher = mock(OrderDtoEnricher.class);
        when(enricher.toOrderDTO(any(), anyList())).thenReturn(new ShopOrderDTO());
        OrderCreator creator = new OrderCreator(
                productMapper, mock(ShopOrderMapper.class), mock(ShopOrderItemMapper.class),
                mock(PickupCodeService.class), new ObjectMapper(),
                mock(OrderDtoConverter.class), mock(OrderRewardService.class), enricher,
                mock(OrderTimeoutIndexer.class), mock(OrderCancelledEventPublisher.class),
                mock(TransactionTemplate.class), mock(OrderPreviewer.class), pricing);
        ReflectionTestUtils.setField(creator, "pointsMallService", pointsMallService);
        return creator;
    }
}
