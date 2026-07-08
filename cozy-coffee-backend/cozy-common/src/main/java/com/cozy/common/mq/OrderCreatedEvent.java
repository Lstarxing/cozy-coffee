package com.cozy.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 下单成功事件载荷。
 * 由 Gateway 在 createOrder 主流程完成后异步发出，
 * 消费端负责触发 SSE 广播、管理端缓存失效等非核心副作用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单主键 */
    private Long orderId;

    /** 订单号（业务号） */
    private String orderNo;

    /** 下单用户 */
    private Long userId;

    /** 用户昵称 / 展示名，用于管理端 SSE 弹窗（可空） */
    private String username;

    /** 实付金额，用于管理端 SSE 展示（可空） */
    private BigDecimal payAmount;

    /** 订单项数量，用于管理端 SSE 展示（可空） */
    private Integer itemCount;

    /** 事件产生时间，便于消费端做超时/幂等判断 */
    private LocalDateTime occurredAt;
}
