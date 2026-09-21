package com.cozy.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单支付成功（= 用户侧接单）事件载荷。
 * <p>
 * 由 Gateway 在 {@code acceptUserOrder} 的 Dubbo 调用返回后发出（provider 事务已提交），
 * 消费端据此**清管理端缓存并广播 new_order** —— 即「商家可以开始做这一单了」。
 * <p>
 * 为什么不复用 {@link OrderCreatedEvent}：两者的 DTO 同构，但语义完全不同
 * （下单 ≠ 已付款），共用会让 {@code RocketMQListener<OrderCreatedEvent>} 出现在
 * "支付"消费者上，读者必然混淆。字段只保留有消费方的那些。
 * <p>
 * 尽力而为：不入 outbox，丢了最坏退化为管理端晚一个缓存 TTL（30s）才看到。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单主键 */
    private Long orderId;

    /** 订单号（业务号） */
    private String orderNo;

    /** 用户昵称 / 展示名，用于管理端 SSE 提示文案（可空） */
    private String username;

    /** 实付金额，用于管理端 SSE 提示文案（可空） */
    private BigDecimal payAmount;

    /** 订单项数量，用于管理端 SSE 提示文案（可空） */
    private Integer itemCount;
}
