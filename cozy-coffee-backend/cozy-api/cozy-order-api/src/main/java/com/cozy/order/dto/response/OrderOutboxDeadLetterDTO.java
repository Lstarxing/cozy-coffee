package com.cozy.order.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 订单服务 DEAD outbox 的管理端只读视图（不暴露完整消息 payload）。 */
@Data
public class OrderOutboxDeadLetterDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long aggregateId;
    private String messageType;
    private String topic;
    private String tag;
    private String status;
    private Integer retryCount;
    private Integer manualRetryCount;
    private String lastError;
    private LocalDateTime nextRetryAt;
    private LocalDateTime lastManualRetryAt;
    private Long lastManualRetryBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
