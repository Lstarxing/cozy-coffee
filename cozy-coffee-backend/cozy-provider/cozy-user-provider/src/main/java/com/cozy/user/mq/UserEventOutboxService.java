package com.cozy.user.mq;

import com.cozy.common.mq.BirthdaySetEvent;
import com.cozy.common.mq.InviteRewardEarnedEvent;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.ProfileCompletedEvent;
import com.cozy.common.mq.UserCreatedEvent;
import com.cozy.common.mq.UserLifecycleEvent;
import com.cozy.common.mq.WelcomeGiftEligibleEvent;
import com.cozy.common.tx.AfterCommit;
import com.cozy.user.entity.UserEventOutbox;
import com.cozy.user.mapper.UserEventOutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventOutboxService {

    private static final int MAX_RETRY = 5;
    private static final Map<String, Class<? extends UserLifecycleEvent>> ALLOWED_EVENTS = Map.of(
            MqTags.USER_CREATED, UserCreatedEvent.class,
            MqTags.WELCOME_GIFT_ELIGIBLE, WelcomeGiftEligibleEvent.class,
            MqTags.PROFILE_COMPLETED, ProfileCompletedEvent.class,
            MqTags.BIRTHDAY_SET, BirthdaySetEvent.class,
            MqTags.INVITE_REWARD_EARNED, InviteRewardEarnedEvent.class);

    private final UserEventOutboxMapper outboxMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private final AtomicLong pendingGauge = new AtomicLong();
    private final AtomicLong deadGauge = new AtomicLong();
    private final AtomicLong oldestPendingAgeSeconds = new AtomicLong();

    @PostConstruct
    void registerGauges() {
        Gauge.builder("cozy.user.event_outbox.pending", pendingGauge, AtomicLong::get)
                .description("待投递的用户生命周期事件数")
                .register(meterRegistry);
        Gauge.builder("cozy.user.event_outbox.dead", deadGauge, AtomicLong::get)
                .description("重试耗尽、需人工重放的用户生命周期事件数")
                .register(meterRegistry);
        Gauge.builder("cozy.user.event_outbox.oldest_pending_age_seconds",
                        oldestPendingAgeSeconds, AtomicLong::get)
                .description("最老待投递用户生命周期事件的滞留秒数")
                .register(meterRegistry);
    }

    /**
     * 在调用方本地事务中持久化事实事件；提交后立即尝试发送，失败由 relay 兜底。
     */
    @Transactional
    public void publish(String tag, Long userId, UserLifecycleEvent event) {
        validate(tag, userId, event);
        String uniqueKey = event.getUniqueKey();

        LocalDateTime now = LocalDateTime.now();
        UserEventOutbox row = new UserEventOutbox();
        row.setTag(tag);
        row.setUniqueKey(uniqueKey);
        row.setUserId(userId);
        row.setStatus("PENDING");
        row.setRetryCount(0);
        row.setNextRetryAt(now);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        try {
            row.setPayload(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new IllegalStateException("用户事件序列化失败: tag=" + tag + ", uniqueKey=" + uniqueKey, e);
        }

        try {
            outboxMapper.insert(row);
        } catch (DuplicateKeyException e) {
            // 数据库唯一键 (tag, unique_key) 是最终并发兜底；已入队即视为成功。
            log.info("用户事件已入队，跳过: tag={}, uniqueKey={}", tag, uniqueKey);
            return;
        }
        AfterCommit.run(() -> trySend(row));
    }

    @Scheduled(fixedDelay = 30000)
    public void relayPending() {
        refreshGauges();
        var pending = outboxMapper.selectPendingBatch(LocalDateTime.now(), 100);
        if (pending.isEmpty()) {
            return;
        }
        log.info("用户事件 outbox 兜底扫描到 {} 条待重投", pending.size());
        for (UserEventOutbox row : pending) {
            if (row.getRetryCount() >= MAX_RETRY) {
                log.error("用户事件重试 {} 次仍失败，标记 DEAD: id={}, tag={}, uniqueKey={}",
                        MAX_RETRY, row.getId(), row.getTag(), row.getUniqueKey());
                row.setStatus("DEAD");
                row.setUpdatedAt(LocalDateTime.now());
                outboxMapper.updateById(row);
                continue;
            }
            if (!trySend(row)) {
                int retry = row.getRetryCount() + 1;
                row.setRetryCount(retry);
                row.setNextRetryAt(LocalDateTime.now().plusSeconds(backoffSeconds(retry)));
                row.setUpdatedAt(LocalDateTime.now());
                outboxMapper.updateById(row);
            }
        }
    }

    private void validate(String tag, Long userId, UserLifecycleEvent event) {
        Class<? extends UserLifecycleEvent> expectedType = ALLOWED_EVENTS.get(tag);
        if (expectedType == null) {
            throw new IllegalArgumentException("不允许的用户事件 tag: " + tag);
        }
        if (event == null || !expectedType.isInstance(event)) {
            throw new IllegalArgumentException("用户事件 tag 与 payload 类型不匹配: " + tag);
        }
        if (userId == null || event.getUniqueKey() == null || event.getUniqueKey().isBlank()
                || event.getOccurredAt() == null) {
            throw new IllegalArgumentException("用户事件 userId、uniqueKey 和 occurredAt 不能为空");
        }
    }

    private void refreshGauges() {
        try {
            pendingGauge.set(outboxMapper.countByStatus("PENDING"));
            deadGauge.set(outboxMapper.countByStatus("DEAD"));
            LocalDateTime oldest = outboxMapper.oldestPendingCreatedAt();
            oldestPendingAgeSeconds.set(
                    oldest == null ? 0 : Math.max(0, Duration.between(oldest, LocalDateTime.now()).getSeconds()));
        } catch (Exception e) {
            log.warn("刷新用户事件 outbox 指标失败: {}", e.getMessage());
        }
    }

    private boolean trySend(UserEventOutbox row) {
        String destination = MqTopics.USER_EVENTS + ":" + row.getTag();
        try {
            rocketMQTemplate.syncSend(destination,
                    MessageBuilder.withPayload(row.getPayload())
                            .setHeader("KEYS", row.getUniqueKey())
                            .setHeader("OUTBOX_ID", String.valueOf(row.getId()))
                            .build());
            row.setStatus("SENT");
            row.setUpdatedAt(LocalDateTime.now());
            outboxMapper.updateById(row);
            log.info("用户事件投递成功: id={}, tag={}, uniqueKey={}",
                    row.getId(), row.getTag(), row.getUniqueKey());
            return true;
        } catch (Exception e) {
            log.warn("用户事件投递失败，等待重投: id={}, tag={}, uniqueKey={}, error={}",
                    row.getId(), row.getTag(), row.getUniqueKey(), e.getMessage());
            return false;
        }
    }

    private long backoffSeconds(int retry) {
        return switch (retry) {
            case 1 -> 10;
            case 2 -> 30;
            case 3 -> 60;
            case 4 -> 120;
            default -> 300;
        };
    }
}
