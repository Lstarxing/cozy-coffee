package com.cozy.common.mq;

import java.io.Serializable;
import java.time.LocalDateTime;

/** USER_EVENTS 载荷的最小公共封套。 */
public interface UserLifecycleEvent extends Serializable {

    String getUniqueKey();

    LocalDateTime getOccurredAt();
}
