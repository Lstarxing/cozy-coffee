package com.cozy.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单取消事件载荷。
 * 由 OrderServiceImpl.cancelOrder / cancelUserOrder 在事务内写入 outbox 表，
 * 异步重投到 MQ 后由 mall-provider 消费，调用 rollbackCoupon。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long userId;

    /** 主券 ID（可空） */
    private Long appliedCouponId;

    /** 附加券 ID 列表（可空） */
    private List<Long> addonCouponIds;

    private LocalDateTime occurredAt;
}
