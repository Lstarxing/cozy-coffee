package com.cozy.order.service;

import com.cozy.mall.api.PointsMallService;
import com.cozy.mall.dto.response.CouponCombinationResult;
import com.cozy.member.api.MemberService;
import com.cozy.member.dto.response.MemberDTO;
import com.cozy.order.api.OrderService;
import com.cozy.order.dto.request.CartCheckRequest;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.request.OrderItemRequest;
import com.cozy.order.dto.response.CartCheckResultDTO;
import com.cozy.order.dto.response.ShopOrderDTO;
import com.cozy.order.service.impl.OrderCreationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 进程内集成测试（Plan C）：验证「预览 checkCart + 下单 createOrder 用券 → 折扣/实付一致且正确」。
 * 跨服务（mall 券组合、member 会员）用 @MockBean 替代，不依赖 live Dubbo；
 * 真实 cozy_order 库（Docker MySQL）+ @Transactional 回滚。
 * 覆盖：主券折扣、主券+辅券叠加、无券三种场景，断言 preview 与 create 口径一致。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderCouponFlowIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderPreviewService previewService;

    @Autowired
    private OrderCreationService creationService;

    @MockBean
    private PointsMallService pointsMallService;

    @MockBean
    private MemberService memberService;

    private static final Long USER_ID = 43L;
    private static final String MEMBER_LEVEL = "diamond";

    @BeforeEach
    void setUp() {
        // @DubboReference 字段不走 Spring bean 注入（@MockBean 不生效），用反射注入 mock 到目标服务
        ReflectionTestUtils.setField(previewService, "pointsMallService", pointsMallService);
        ReflectionTestUtils.setField(previewService, "memberService", memberService);
        ReflectionTestUtils.setField(creationService, "pointsMallService", pointsMallService);
        ReflectionTestUtils.setField(creationService, "memberService", memberService);

        MemberDTO member = new MemberDTO();
        member.setMemberLevel(MEMBER_LEVEL);
        member.setExpTotal(4100);
        when(memberService.getMemberByUserId(anyLong())).thenReturn(member);
    }

    @Test
    void previewAndCreateOrder_withMainDiscount_consistent() {
        stubCombination(discount(11, 0, 0), 999L, "DISCOUNT");
        CreateOrderRequest request = orderRequest("CPN_MAIN");

        CartCheckResultDTO check = orderService.checkCart(USER_ID, MEMBER_LEVEL, toCartCheck(request));
        assertEquals(0, new BigDecimal("11").compareTo(check.getPreview().getDiscount()));
        assertEquals(0, new BigDecimal("11").compareTo(check.getPreview().getPayable()));

        ShopOrderDTO order = orderService.createOrder(USER_ID, MEMBER_LEVEL, "it-main-coupon", request);
        assertEquals(0, new BigDecimal("11").compareTo(order.getPayAmount()));
        // preview 与 create 口径一致
        assertEquals(0, check.getPreview().getPayable().compareTo(order.getPayAmount()));
    }

    @Test
    void previewAndCreateOrder_withMainPlusAddon_consistent() {
        // 主券折扣 11 + 辅券（SHOT 加浓缩）5 → 总折扣 16，实付 6
        stubCombination(discount(11, 5, 0), 999L, "DISCOUNT");
        CreateOrderRequest request = orderRequest("CPN_MAIN_ADDON");
        request.setAddonCouponCodes(Collections.singletonList("CPN_SHOT"));

        CartCheckResultDTO check = orderService.checkCart(USER_ID, MEMBER_LEVEL, toCartCheck(request));
        assertEquals(0, new BigDecimal("16").compareTo(check.getPreview().getDiscount()));
        assertEquals(0, new BigDecimal("6").compareTo(check.getPreview().getPayable()));

        ShopOrderDTO order = orderService.createOrder(USER_ID, MEMBER_LEVEL, "it-main-addon", request);
        assertEquals(0, new BigDecimal("6").compareTo(order.getPayAmount()));
    }

    @Test
    void createOrder_withoutCoupon_usesSubtotal() {
        CreateOrderRequest request = orderRequest(null);

        CartCheckResultDTO check = orderService.checkCart(USER_ID, MEMBER_LEVEL, toCartCheck(request));
        assertEquals(0, BigDecimal.ZERO.compareTo(check.getPreview().getDiscount()));
        assertEquals(0, new BigDecimal("22").compareTo(check.getPreview().getPayable()));

        ShopOrderDTO order = orderService.createOrder(USER_ID, MEMBER_LEVEL, "it-no-coupon", request);
        assertEquals(0, new BigDecimal("22").compareTo(order.getPayAmount()));
    }

    // ==================== 工具 ====================

    private void stubCombination(CouponCombinationResult combo, Long mainCouponId, String mainType) {
        combo.setMainCouponId(mainCouponId);
        combo.setMainCouponType(mainType);
        when(pointsMallService.previewCouponCombination(anyLong(), anyList(),
                any(BigDecimal.class), any(BigDecimal.class), anyList(), anyList())).thenReturn(combo);
        when(pointsMallService.useCouponCombination(anyLong(), anyList(),
                any(BigDecimal.class), any(BigDecimal.class), anyList(), anyList())).thenReturn(combo);
    }

    private CouponCombinationResult discount(int main, int addon, int deliveryFee) {
        CouponCombinationResult combo = new CouponCombinationResult();
        combo.setMainDiscount(new BigDecimal(main));
        combo.setAddonDiscount(new BigDecimal(addon));
        combo.setDeliveryFeeDiscount(new BigDecimal(deliveryFee));
        return combo;
    }

    private CreateOrderRequest orderRequest(String couponCode) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(19L); // espresso, price 22, sizeType MEDIUM_LARGE
        item.setQuantity(1);
        item.setCupSize("MEDIUM");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(Collections.singletonList(item));
        request.setDiningMethod("TAKEOUT");
        request.setCouponCode(couponCode);
        return request;
    }

    private CartCheckRequest toCartCheck(CreateOrderRequest request) {
        CartCheckRequest check = new CartCheckRequest();
        check.setItems(request.getItems());
        check.setCouponCode(request.getCouponCode());
        check.setAddonCouponCodes(request.getAddonCouponCodes());
        check.setStoreId(1L);
        check.setPickupTime("ASAP");
        return check;
    }
}
