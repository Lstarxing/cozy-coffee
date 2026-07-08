package com.cozy.common.mq;

/**
 * RocketMQ 标签常量。
 * 本期仅接入 ORDER_CREATED，其余标签保留位置以便后续扩展。
 */
public final class MqTags {

    private MqTags() {
    }

    public static final String ORDER_CREATED = "order_created";
    public static final String ORDER_COMPLETED = "order_completed";
    public static final String ORDER_CANCELLED = "order_cancelled";
}
