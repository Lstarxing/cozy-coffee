package com.cozy.mall.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 积分退款 DEAD outbox 的管理端只读视图。 */
@Data
public class PointsRefundDeadLetterDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private Long userId;
    private Integer points;
    private String consumeType;
    private String description;
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
