package com.cozy.order.service;
import com.cozy.order.service.product.ProductPricingService;
import com.cozy.order.service.order.OrderPreviewer;
import com.cozy.order.service.product.ProductAddonResolver;
import com.cozy.order.service.product.ProductRuleValidator;

import com.cozy.member.api.MemberService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.mall.api.PointsMallService;
import com.cozy.mall.dto.request.ItemCheckDTO;
import com.cozy.order.dto.request.CartCheckRequest;
import com.cozy.order.dto.request.OrderItemRequest;
import com.cozy.order.dto.response.CartCheckResultDTO;
import com.cozy.order.entity.CoffeeProduct;
import com.cozy.order.mapper.CoffeeProductAddonGroupMapper;
import com.cozy.order.mapper.CoffeeProductAddonMapper;
import com.cozy.order.mapper.CoffeeProductMapper;
import com.cozy.order.mapper.ProductAddonMapper;
import com.cozy.order.service.order.OrderRewardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class OrderPreviewerTest {

    private CoffeeProductMapper productMapper;
    private OrderRewardService rewardService;
    private OrderPreviewer service;

    @BeforeEach
    void setUp() {
        productMapper = mock(CoffeeProductMapper.class);
        rewardService = mock(OrderRewardService.class);
        ProductAddonResolver addonResolver = new ProductAddonResolver(
                mock(CoffeeProductAddonGroupMapper.class),
                mock(CoffeeProductAddonMapper.class),
                mock(ProductAddonMapper.class),
                new ObjectMapper());
        ProductPricingService pricing = new ProductPricingService(new ProductRuleValidator(), addonResolver);
        service = new OrderPreviewer(productMapper, new ObjectMapper(), rewardService, pricing);
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
        Field field = OrderPreviewer.class.getDeclaredField("memberService");
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
        product.setPriceMedium(new BigDecimal(price));
        product.setPriceLarge(new BigDecimal(price).add(new BigDecimal("3")));
        product.setCategory("coffee");
        product.setSizeType("MEDIUM_LARGE");
        product.setSugarType("FREE_CHOICE");
        product.setTempType("HOT_COLD");
        product.setUpdatedAt(LocalDateTime.of(2026, 7, 14, 12, 0));
        return product;
    }

    /**
     * ItemCheckDTO.price 的契约守卫（见 docs/adr/0002）：它必须是 order 权威算出的
     * 【当前规格】【每单位】基础价，不含加料费。
     *
     * <p>mall 侧的券估值只认这个字段（指定商品兑换券不再反查 order 的商品目录），
     * 所以这里钉住"每单位 + 按规格"两条：数量 2、LARGE 杯时必须传 23（= priceLarge），
     * 而不是整行 46、也不是标准杯价。
     */
    @Test
    @SuppressWarnings("unchecked")
    void itemCheckPriceIsUnitBasePriceOfChosenSpec() {
        when(productMapper.selectById(13L)).thenReturn(product(13L, "active", "20.00"));
        PointsMallService mall = mock(PointsMallService.class);
        ReflectionTestUtils.setField(service, "pointsMallService", mall);

        CartCheckRequest req = request(item(13L, 2, "LARGE"));
        req.setCouponCode("C1");
        service.preview(7L, "basic", req);

        ArgumentCaptor<List<ItemCheckDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(mall).previewCouponCombination(any(), anyList(), any(), any(), anyList(), captor.capture());

        assertEquals(0, new BigDecimal("23.00").compareTo(captor.getValue().get(0).getPrice()));
    }
}
