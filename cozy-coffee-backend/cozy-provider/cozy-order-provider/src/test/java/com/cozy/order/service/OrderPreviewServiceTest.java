package com.cozy.order.service;

import com.cozy.member.api.MemberService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.order.dto.request.CartCheckRequest;
import com.cozy.order.dto.request.OrderItemRequest;
import com.cozy.order.dto.response.CartCheckResultDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.service.impl.OrderRewardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderPreviewServiceTest {

    private CoffeeProductMapper productMapper;
    private OrderRewardService rewardService;
    private OrderPreviewService service;

    @BeforeEach
    void setUp() {
        productMapper = mock(CoffeeProductMapper.class);
        rewardService = mock(OrderRewardService.class);
        service = new OrderPreviewService(productMapper, new ProductSkuValidationService(), new ObjectMapper(), rewardService);
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

    @Test
    void previewReturnsPointsEstimateFromBackend() throws Exception {
        when(productMapper.selectById(20L)).thenReturn(product(20L, "active", "20.00"));

        MemberService memberService = mock(MemberService.class);
        MemberDTO member = new MemberDTO();
        member.setMemberLevel("gold");
        member.setExpTotal(1600);
        when(memberService.getMemberByUserId(7L)).thenReturn(member);
        Field field = OrderPreviewService.class.getDeclaredField("memberService");
        field.setAccessible(true);
        field.set(service, memberService);

        OrderRewardService.RewardEstimate est = new OrderRewardService.RewardEstimate();
        est.expEarned = 20;
        est.pointsEarned = 24;
        when(rewardService.estimateRewards(any(), any())).thenReturn(est);

        CartCheckResultDTO result = service.preview(7L, "gold", request(item(20L, 1, "STANDARD")));

        assertEquals(Integer.valueOf(24), result.getPreview().getPointsEarned());
        assertEquals(Integer.valueOf(20), result.getPreview().getExpEarned());
    }

    @Test
    void previewFallsBackToOneToOneWhenMemberUnavailable() {
        when(productMapper.selectById(21L)).thenReturn(product(21L, "active", "20.00"));
        // memberService 未注入 → 预估失败回退 1:1（points=exp=payable）
        CartCheckResultDTO result = service.preview(7L, "basic", request(item(21L, 1, "STANDARD")));
        assertEquals(Integer.valueOf(20), result.getPreview().getPointsEarned());
        assertEquals(Integer.valueOf(20), result.getPreview().getExpEarned());
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
        product.setTempType("HOT_COLD");
        product.setUpdatedAt(LocalDateTime.of(2026, 7, 14, 12, 0));
        return product;
    }
}
