package com.cozy.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单完成事件载荷。
 * 由 Gateway 在 completeOrder 主流程完成后异步发出，
 * 消费端（member-provider）负责积分/EXP 发放、首单奖励、月度任务更新。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderNo;
    private Long userId;
    private BigDecimal payAmount;
    private Integer expEarned;
    private Integer pointsEarned;

    /** 首单标记（由 OrderServiceImpl 在设置 completed 状态前判定） */
    private Boolean isFirstOrder;

    /** 订单中是否包含新品商品 */
    private Boolean hasNewProduct;

    /** 是否为外卖订单（diningMethod == DELIVERY） */
    private Boolean isDelivery;

    private LocalDateTime occurredAt;
}
