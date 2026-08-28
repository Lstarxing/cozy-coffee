package com.cozy.order.service;

import com.cozy.common.exception.BusinessException;

/**
 * 订单状态机（Phase 4.1）。
 * 从 OrderServiceImpl 多个方法中分散的 status.equals() 字符串比较
 * 集中到枚举 + 转换校验。
 */
public enum OrderStateMachine {

    PENDING("pending"),
    PREPARING("preparing"),
    DELIVERING("delivering"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String value;

    OrderStateMachine(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static OrderStateMachine from(String status) {
        for (OrderStateMachine s : values()) {
            if (s.value.equalsIgnoreCase(status)) return s;
        }
        throw new IllegalArgumentException("无效订单状态: " + status);
    }

    /**
     * 校验状态流转是否合法。
     * pending -> preparing (acceptOrder)
     * preparing -> delivering (外送出餐) / completed (自提出餐) / cancelled
     * delivering -> completed (到点自动完成) / cancelled
     */
    public void assertCanTransition(OrderStateMachine to) {
        boolean valid = switch (this) {
            case PENDING -> to == PREPARING || to == CANCELLED;
            case PREPARING -> to == DELIVERING || to == COMPLETED || to == CANCELLED;
            case DELIVERING -> to == COMPLETED || to == CANCELLED;
            default -> false;
        };
        if (!valid) {
            throw new BusinessException(
                    "订单状态流转不合法: " + this.value + " -> " + to.value);
        }
    }
}
