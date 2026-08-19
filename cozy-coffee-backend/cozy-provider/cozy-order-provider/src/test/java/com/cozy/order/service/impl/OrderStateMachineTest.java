package com.cozy.order.service.impl;

import com.cozy.common.exception.BusinessException;
import com.cozy.order.service.order.OrderStateMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStateMachineTest {

    @Test
    void from_parsesValidStatusCaseInsensitive() {
        assertEquals(OrderStateMachine.PENDING, OrderStateMachine.from("pending"));
        assertEquals(OrderStateMachine.PREPARING, OrderStateMachine.from("PREPARING"));
        assertEquals(OrderStateMachine.DELIVERING, OrderStateMachine.from("Delivering"));
        assertEquals(OrderStateMachine.COMPLETED, OrderStateMachine.from("completed"));
        assertEquals(OrderStateMachine.CANCELLED, OrderStateMachine.from("cancelled"));
    }

    @Test
    void from_rejectsUnknownStatus() {
        assertThrows(IllegalArgumentException.class, () -> OrderStateMachine.from("unknown"));
        assertThrows(IllegalArgumentException.class, () -> OrderStateMachine.from(null));
    }

    @Test
    void value_returnsWireValue() {
        assertEquals("pending", OrderStateMachine.PENDING.value());
        assertEquals("preparing", OrderStateMachine.PREPARING.value());
        assertEquals("delivering", OrderStateMachine.DELIVERING.value());
        assertEquals("completed", OrderStateMachine.COMPLETED.value());
        assertEquals("cancelled", OrderStateMachine.CANCELLED.value());
    }

    @Test
    void validTransitionsAreAllowed() {
        assertDoesNotThrow(() -> OrderStateMachine.PENDING.assertCanTransition(OrderStateMachine.PREPARING));
        assertDoesNotThrow(() -> OrderStateMachine.PENDING.assertCanTransition(OrderStateMachine.CANCELLED));
        assertDoesNotThrow(() -> OrderStateMachine.PREPARING.assertCanTransition(OrderStateMachine.DELIVERING));
        assertDoesNotThrow(() -> OrderStateMachine.PREPARING.assertCanTransition(OrderStateMachine.COMPLETED));
        assertDoesNotThrow(() -> OrderStateMachine.PREPARING.assertCanTransition(OrderStateMachine.CANCELLED));
        assertDoesNotThrow(() -> OrderStateMachine.DELIVERING.assertCanTransition(OrderStateMachine.COMPLETED));
        assertDoesNotThrow(() -> OrderStateMachine.DELIVERING.assertCanTransition(OrderStateMachine.CANCELLED));
    }

    @Test
    void invalidTransitionsThrowBusinessException() {
        // 自环
        assertThrows(BusinessException.class, () -> OrderStateMachine.PENDING.assertCanTransition(OrderStateMachine.PENDING));
        assertThrows(BusinessException.class, () -> OrderStateMachine.PREPARING.assertCanTransition(OrderStateMachine.PREPARING));
        assertThrows(BusinessException.class, () -> OrderStateMachine.DELIVERING.assertCanTransition(OrderStateMachine.DELIVERING));
        // 跳级
        assertThrows(BusinessException.class, () -> OrderStateMachine.PENDING.assertCanTransition(OrderStateMachine.COMPLETED));
        assertThrows(BusinessException.class, () -> OrderStateMachine.PENDING.assertCanTransition(OrderStateMachine.DELIVERING));
        // 回退
        assertThrows(BusinessException.class, () -> OrderStateMachine.DELIVERING.assertCanTransition(OrderStateMachine.PREPARING));
        assertThrows(BusinessException.class, () -> OrderStateMachine.DELIVERING.assertCanTransition(OrderStateMachine.PENDING));
        assertThrows(BusinessException.class, () -> OrderStateMachine.COMPLETED.assertCanTransition(OrderStateMachine.PREPARING));
        assertThrows(BusinessException.class, () -> OrderStateMachine.COMPLETED.assertCanTransition(OrderStateMachine.CANCELLED));
        // 终态不可再流转
        assertThrows(BusinessException.class, () -> OrderStateMachine.CANCELLED.assertCanTransition(OrderStateMachine.PENDING));
        assertThrows(BusinessException.class, () -> OrderStateMachine.CANCELLED.assertCanTransition(OrderStateMachine.COMPLETED));
    }

    @Test
    void invalidTransitionMessageContainsBothStates() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> OrderStateMachine.PENDING.assertCanTransition(OrderStateMachine.COMPLETED));
        assertTrue(ex.getMessage().contains("pending"));
        assertTrue(ex.getMessage().contains("completed"));
        assertTrue(ex.getMessage().contains("订单状态流转不合法"));
    }
}
