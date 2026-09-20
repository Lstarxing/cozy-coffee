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
public class BirthdaySetEvent implements UserLifecycleEvent {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Integer benefitYear;
    private String uniqueKey;
    private LocalDateTime occurredAt;
}
