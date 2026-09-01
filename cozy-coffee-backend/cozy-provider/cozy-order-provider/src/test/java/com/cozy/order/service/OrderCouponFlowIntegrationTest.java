package com.cozy.order.service;
import com.cozy.order.service.order.OrderPreviewer;

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
import com.cozy.order.service.order.OrderCreator;
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
    private OrderPreviewer previewService;

    @Autowired
    private OrderCreator creationService;

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
        // 落库的每张券明细应随 createOrder 返回（订单详情页逐条展示）
        assertEquals(2, order.getCouponDetails().size());
        assertEquals("主券", order.getCouponDetails().get(0).getTitle());
        assertEquals(0, new BigDecimal("11").compareTo(order.getCouponDetails().get(0).getDiscount()));
        assertEquals("辅券", order.getCouponDetails().get(1).getTitle());
        assertEquals(0, new BigDecimal("5").compareTo(order.getCouponDetails().get(1).getDiscount()));
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

    @Test
    void previewAndCreateOrder_deliveryWithDeliveryFeeCoupon_consistent() {
        // 外送：配送费 3 元，配送费券抵扣 3 元 → 实付 = 22 − 0 − 3 + 3 = 22
        stubCombination(discount(0, 0, 3), 999L, "DELIVERY_FEE");
        CreateOrderRequest request = orderRequest("CPN_DELIVERY_FEE");
        request.setDiningMethod("DELIVERY");
        request.setAddonCouponCodes(Collections.singletonList("CPN_DELIVERY_FEE"));

        CartCheckResultDTO check = orderService.checkCart(USER_ID, MEMBER_LEVEL, toCartCheck(request));
        // 配送费券计入优惠合计，实付 = 小计 − 折扣 + 配送费 = 22 − 3 + 3
        assertEquals(0, new BigDecimal("3").compareTo(check.getPreview().getDiscount()));
        assertEquals(0, new BigDecimal("3").compareTo(check.getPreview().getDeliveryFee()));
        assertEquals(0, new BigDecimal("22").compareTo(check.getPreview().getPayable()));

        ShopOrderDTO order = orderService.createOrder(USER_ID, MEMBER_LEVEL, "it-delivery-fee-coupon", request);
        assertEquals(0, new BigDecimal("22").compareTo(order.getPayAmount()));
        // preview 与 create 口径一致
        assertEquals(0, check.getPreview().getPayable().compareTo(order.getPayAmount()));
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
        // 组装每张券明细（title/discount），供下单落库 couponDetails 验证
        if (main > 0) combo.getDetails().add(detail("主券", main, true));
        if (addon > 0) combo.getDetails().add(detail("辅券", addon, false));
        if (deliveryFee > 0) combo.getDetails().add(detail("配送费券", deliveryFee, false));
        return combo;
    }

    private CouponCombinationResult.CouponDetail detail(String title, int discount, boolean main) {
        CouponCombinationResult.CouponDetail d = new CouponCombinationResult.CouponDetail();
        d.setTitle(title);
        d.setDiscount(new BigDecimal(discount));
        d.setMain(main);
        return d;
    }

    private CreateOrderRequest orderRequest(String couponCode) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(36L); // Cozy 美式, ESPRESSO, MEDIUM_LARGE, medium price 22
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
        check.setDiningMethod(request.getDiningMethod());
        check.setDeliveryAddressId(request.getDeliveryAddressId());
        return check;
    }
}
