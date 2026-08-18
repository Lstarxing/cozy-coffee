package com.cozy.order.service;

import com.cozy.order.api.OrderService;
import com.cozy.order.dto.request.CreateOrderRequest;
import com.cozy.order.dto.request.OrderItemRequest;
import com.cozy.order.dto.response.ShopOrderDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Baseline test capturing current order flow behavior BEFORE any refactor.
 * Phase 1+ refactors MUST NOT break this test -- if they do, they changed behavior.
 *
 * Scope (Phase 0): single happy-path with BOGO coupon, complete + cancel flows.
 * Cancel flow + remaining 4 coupon types added in Phase 1+ alongside each fix.
 *
 * Re-runnability notes:
 * - createOrder makes a real Dubbo RPC to cozy-mall-provider (running in IDE) to
 *   consume coupon 6M9E9JJE. That RPC commits in mall-provider's JVM, so the
 *   test's @Transactional rollback cannot undo the coupon state change.
 * - @BeforeTransaction resets the coupon to ISSUED before each run via a cross-DB
 *   UPDATE (root user can write to cozy_mall from the cozy_order datasource).
 * - @Transactional rolls back local cozy_order writes (shop_order, shop_order_item,
 *   pickup_code_counter), preventing order DB pollution across runs.
 *
 * Status flow (verified against current OrderServiceImpl):
 *   createOrder  -> "pending"    (待支付，15 分钟超时自动取消)
 *   acceptOrder  -> "preparing"  (支付成功后自动接单；completeOrder 前必须为 preparing)
 *   completeOrder -> "completed"
 *   pending/preparing -> cancelOrder -> "cancelled"
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderFlowBaselineTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BOGO_COUPON_CODE = "6M9E9JJE";

    @BeforeTransaction
    public void resetCouponState() {
        // The Dubbo RPC to mall-provider commits independently of the test's
        // @Transactional rollback, so reset the coupon before each run.
        // Also bump expires_at to a future date: coupon 6M9E9JJE was issued
        // 2026-01-10 with 7-day validity, so by Phase 0 (2026-07) it is
        // date-expired even though status='ISSUED'. This is test-fixture setup
        // (analogous to the brief's allowed "insert test BOGO coupon" step),
        // not a production code change.
        jdbcTemplate.update(
                "UPDATE cozy_mall.user_coupons "
                        + "SET status='ISSUED', used_at=NULL, used_shop_order_id=NULL, "
                        + "    expires_at=DATE_ADD(NOW(), INTERVAL 365 DAY) "
                        + "WHERE coupon_code=?",
                BOGO_COUPON_CODE);
    }

    @Test
    public void testCreateAndCompleteOrder_bogoCoupon_baseline() {
        Long userId = 43L;          // existing diamond member, current_points=3707
        String memberLevel = "diamond";

        CreateOrderRequest request = buildBogoOrderRequest();

        // Act: create order -> status "pending"（待支付）
        ShopOrderDTO created = orderService.createOrder(userId, memberLevel, null, request);
        assertNotNull(created.getId(), "Order should have an ID after creation");
        assertEquals("pending", created.getStatus(), "New order should be pending (awaiting payment)");

        // Act: accept order -> status "preparing"（支付成功后自动接单）
        ShopOrderDTO accepted = orderService.acceptOrder(created.getId());
        assertEquals("preparing", accepted.getStatus(), "Order should be preparing after accept");

        // Act: complete order -> status "completed"
        ShopOrderDTO completed = orderService.completeOrder(created.getId());
        assertEquals("completed", completed.getStatus(), "Order should be completed");
    }

    @Test
    public void testCreateAndCancelOrder_noCoupon_baseline() {
        Long userId = 43L;
        String memberLevel = "diamond";

        // Create order without coupon
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(19L);
        item.setQuantity(1);
        item.setCupSize("MEDIUM");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(Collections.singletonList(item));
        request.setDiningMethod("TAKEOUT");

        ShopOrderDTO created = orderService.createOrder(userId, memberLevel, null, request);
        assertNotNull(created.getId());
        assertEquals("pending", created.getStatus(), "New order should be pending (awaiting payment)");

        // Cancel (admin side)
        ShopOrderDTO cancelled = orderService.cancelOrder(created.getId());
        assertEquals("cancelled", cancelled.getStatus(), "Order should be cancelled");
    }

    @Test
    public void testCreateAndAcceptAndCancelOrder_baseline() {
        Long userId = 43L;
        String memberLevel = "diamond";

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(19L);
        item.setQuantity(1);
        item.setCupSize("MEDIUM");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(Collections.singletonList(item));
        request.setDiningMethod("TAKEOUT");

        ShopOrderDTO created = orderService.createOrder(userId, memberLevel, null, request);
        assertEquals("pending", created.getStatus(), "New order should be pending (awaiting payment)");

        // Accept -> "preparing"（支付成功后自动接单）
        ShopOrderDTO accepted = orderService.acceptOrder(created.getId());
        assertEquals("preparing", accepted.getStatus());

        // Cancel from preparing state (admin side)
        ShopOrderDTO cancelled = orderService.cancelOrder(created.getId());
        assertEquals("cancelled", cancelled.getStatus(), "Preparing order should be cancellable");
    }

    private CreateOrderRequest buildBogoOrderRequest() {
        // Product 19: status=active, size_type=MEDIUM_LARGE, category=espresso, price=22.00
        // cupSize "MEDIUM" is allowed for MEDIUM_LARGE (case-insensitive match on "medium").
        // quantity=2: BOGO coupon rule (PointsMallServiceImpl.calculateCouponDiscount BOGO branch)
        // requires drinkPrices.size() >= 2 -- buy-one-get-one needs 2 cups in the cart.
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(19L);
        item.setQuantity(2);
        item.setCupSize("MEDIUM");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(Collections.singletonList(item));
        request.setCouponCode(BOGO_COUPON_CODE);
        request.setDiningMethod("TAKEOUT");
        return request;
    }
}
