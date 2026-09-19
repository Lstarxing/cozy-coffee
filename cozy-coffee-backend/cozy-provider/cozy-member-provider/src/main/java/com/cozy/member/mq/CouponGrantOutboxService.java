package com.cozy.member.mq;

import com.cozy.common.mq.CouponGrantRequestedEvent;
import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.tx.AfterCommit;
import com.cozy.member.entity.CouponGrantOutbox;
import com.cozy.member.mapper.CouponGrantOutboxMapper;
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * 发券请求的本地 Outbox。
 *
 * <p>member 不再同步 RPC 调 mall 发券（那会让 member 依赖 cozy-mall-api，形成反向依赖 + 远程副作用无法回滚），
 * 改为：<b>与业务数据同事务写 outbox → 提交后投递 MQ → mall 消费发券</b>。
 *
 * <p>- {@link #publish} 在调用方事务内 INSERT，唯一键保证同一次发券只入队一次
 * <p>- afterCommit 立即投递只降低延迟；<b>可靠性靠 {@link #relayPending} 定时重投</b>（进程崩溃后仍会补发）
 * <p>- 消费端幂等由 mall 侧 user_coupon.coupon_code 去重保证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponGrantOutboxService {

    private static final int MAX_RETRY = 5;

    private final CouponGrantOutboxMapper outboxMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    // 指标值由 relay tick 刷新（每 30s），避免每次 scrape 都打库
    private final AtomicLong pendingGauge = new AtomicLong();
    private final AtomicLong deadGauge = new AtomicLong();
    private final AtomicLong oldestPendingAgeSeconds = new AtomicLong();

    /**
     * DEAD/PENDING 必须可被监控看见——否则投递失败只剩一行日志，
     * 会把"发券失败"变成静默丢失（比原先同步 RPC 直接报错更危险）。
     * 建议对 dead 与 oldestPendingAgeSeconds 配告警。
     */
    @PostConstruct
    void registerGauges() {
        Gauge.builder("cozy.member.coupon_grant_outbox.pending", pendingGauge, AtomicLong::get)
                .description("待投递的发券请求数")
                .register(meterRegistry);
        Gauge.builder("cozy.member.coupon_grant_outbox.dead", deadGauge, AtomicLong::get)
                .description("重试耗尽、需人工重放的发券请求数")
                .register(meterRegistry);
        Gauge.builder("cozy.member.coupon_grant_outbox.oldest_pending_age_seconds",
                        oldestPendingAgeSeconds, AtomicLong::get)
                .description("最老待投递请求的滞留秒数")
                .register(meterRegistry);
    }

    private void refreshGauges() {
        try {
            pendingGauge.set(outboxMapper.countByStatus("PENDING"));
            deadGauge.set(outboxMapper.countByStatus("DEAD"));
            LocalDateTime oldest = outboxMapper.oldestPendingCreatedAt();
            oldestPendingAgeSeconds.set(
                    oldest == null ? 0 : Math.max(0, Duration.between(oldest, LocalDateTime.now()).getSeconds()));
        } catch (Exception e) {
            // 指标刷新失败不能影响重投主流程
            log.warn("刷新发券 outbox 指标失败: {}", e.getMessage());
        }
    }

    @Transactional
    public void publish(Long userId, String couponType, String uniqueKey, double minAmount,
                        double discountAmount, int validDays, String source) {
        CouponGrantRequestedEvent event = CouponGrantRequestedEvent.builder()
                .userId(userId)
                .couponType(couponType)
                .uniqueKey(uniqueKey)
                .minAmount(minAmount)
                .discountAmount(discountAmount)
                .validDays(validDays)
                .source(source)
                .build();

        CouponGrantOutbox row = new CouponGrantOutbox();
        row.setUniqueKey(uniqueKey);
        row.setUserId(userId);
        row.setStatus("PENDING");
        row.setRetryCount(0);
        row.setNextRetryAt(LocalDateTime.now());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        try {
            row.setPayload(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new IllegalStateException("发券请求序列化失败: uniqueKey=" + uniqueKey, e);
        }

        try {
            outboxMapper.insert(row);
        } catch (DuplicateKeyException e) {
            // 同一笔发券重复请求（重试/并发），已入队即可
            log.info("发券请求已入队，跳过: uniqueKey={}", uniqueKey);
            return;
        }
        AfterCommit.run(() -> trySend(row));
    }

    /**
     * 兜底任务：每 30s 扫描 PENDING 且到达重试时间的消息重投。
     * 指数退避 10/30/60/120/300s，5 次仍失败转 DEAD 等人工处理。
     */
    @Scheduled(fixedDelay = 30000)
    public void relayPending() {
        refreshGauges();
        var pending = outboxMapper.selectPendingBatch(LocalDateTime.now(), 100);
        if (pending.isEmpty()) {
            return;
        }
        log.info("发券 outbox 兜底扫描到 {} 条待重投", pending.size());
        for (CouponGrantOutbox row : pending) {
            if (row.getRetryCount() >= MAX_RETRY) {
                log.error("发券请求重试 {} 次仍失败，标记 DEAD: id={}, uniqueKey={}",
                        MAX_RETRY, row.getId(), row.getUniqueKey());
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

    private boolean trySend(CouponGrantOutbox row) {
        String destination = MqTopics.MEMBER_EVENTS + ":" + MqTags.COUPON_GRANT_REQUESTED;
        try {
            rocketMQTemplate.syncSend(destination,
                    MessageBuilder.withPayload(row.getPayload())
                            .setHeader("KEYS", row.getUniqueKey())
                            .setHeader("OUTBOX_ID", String.valueOf(row.getId()))
                            .build());
            row.setStatus("SENT");
            row.setUpdatedAt(LocalDateTime.now());
            outboxMapper.updateById(row);
            log.info("发券请求投递成功: id={}, uniqueKey={}", row.getId(), row.getUniqueKey());
            return true;
        } catch (Exception e) {
            // 返回 false 让调用方累计重试；不能吞掉失败否则 retry_count 不增长
            log.warn("发券请求投递失败，等待重投: id={}, uniqueKey={}, error={}",
                    row.getId(), row.getUniqueKey(), e.getMessage());
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
