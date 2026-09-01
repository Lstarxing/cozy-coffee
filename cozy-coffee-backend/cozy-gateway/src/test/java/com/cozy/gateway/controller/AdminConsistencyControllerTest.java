package com.cozy.gateway.controller;

import com.cozy.common.context.UserContext;
import com.cozy.mall.api.PointsMallService;
import com.cozy.order.api.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminConsistencyControllerTest {

    @Mock private OrderService orderService;
    @Mock private PointsMallService pointsMallService;

    private AdminConsistencyController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminConsistencyController();
        ReflectionTestUtils.setField(controller, "orderService", orderService);
        ReflectionTestUtils.setField(controller, "pointsMallService", pointsMallService);
        UserContext.setUserId(88L);
        UserContext.setRole("admin");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void deadCounts_combinesBothServices() {
        when(orderService.countDeadOutboxMessages()).thenReturn(2L);
        when(pointsMallService.countDeadPointRefunds()).thenReturn(3L);

        var result = controller.deadCounts();

        assertEquals(2L, result.getData().get("orderOutbox"));
        assertEquals(3L, result.getData().get("pointsRefund"));
    }

    @Test
    void listEndpoints_forwardLimit() {
        when(orderService.listDeadOutboxMessages(20)).thenReturn(List.of());
        when(pointsMallService.listDeadPointRefunds(30)).thenReturn(List.of());

        controller.listDeadOrderOutbox(20);
        controller.listDeadPointRefunds(30);

        verify(orderService).listDeadOutboxMessages(20);
        verify(pointsMallService).listDeadPointRefunds(30);
    }

    @Test
    void retryEndpoints_recordCurrentAdmin() {
        controller.retryDeadOrderOutbox(10L);
        controller.retryDeadPointRefund(20L);

        verify(orderService).retryDeadOutboxMessage(10L, 88L);
        verify(pointsMallService).retryDeadPointRefund(20L, 88L);
    }
}
