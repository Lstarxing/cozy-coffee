package com.cozy.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCompletedEvent implements UserLifecycleEvent {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String uniqueKey;
    private LocalDateTime occurredAt;
}
