package com.cozy.order.mq;

import com.cozy.order.entity.MessageOutbox;
import com.cozy.order.mapper.MessageOutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * Outbox 模式服务：
 * - {@link #publish} 在调用方事务内 INSERT 一条 PENDING 消息（与订单状态变更原子提交）
 * - 事务提交后立即尝试 sendSync 投递，成功则置 SENT
 * - 兜底由 {@link #relayPendingMessages} 定时扫描重投
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final MessageOutboxMapper outboxMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publish(String topic, String tag, String messageType, Long aggregateId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            MessageOutbox msg = new MessageOutbox();
            msg.setAggregateId(aggregateId);
            msg.setMessageType(messageType);
            msg.setTopic(topic);
            msg.setTag(tag);
            msg.setPayload(json);
            msg.setStatus("PENDING");
            msg.setRetryCount(0);
            msg.setNextRetryAt(LocalDateTime.now());
            msg.setCreatedAt(LocalDateTime.now());
            msg.setUpdatedAt(LocalDateTime.now());
            outboxMapper.insert(msg);
            Long outboxId = msg.getId();

            // 事务提交后异步投递，避免 sendSync 失败导致业务事务回滚
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    trySend(msg);
                }
            });
        } catch (Exception e) {
            log.error("Outbox 写入失败: aggregateId={}, type={}", aggregateId, messageType, e);
            throw new RuntimeException("Outbox 写入失败", e);
        }
    }

    private void trySend(MessageOutbox msg) {
        String destination = msg.getTopic() + ":" + msg.getTag();
        try {
            rocketMQTemplate.syncSend(destination,
                    MessageBuilder.withPayload(msg.getPayload())
                            .setHeader("KEYS", String.valueOf(msg.getAggregateId()))
                            .setHeader("OUTBOX_ID", String.valueOf(msg.getId()))
                            .build());
            msg.setStatus("SENT");
            msg.setUpdatedAt(LocalDateTime.now());
            outboxMapper.updateById(msg);
            log.info("Outbox 消息投递成功: id={}, aggregateId={}", msg.getId(), msg.getAggregateId());
        } catch (Exception e) {
            // 投递失败保留 PENDING，由兜底任务重试
            log.warn("Outbox 投递失败，等待重试: id={}, aggregateId={}, error={}",
                    msg.getId(), msg.getAggregateId(), e.getMessage());
        }
    }

    /**
     * 兜底任务：每 30s 扫描 PENDING 且 next_retry_at <= now 的消息重投。
     * 指数退避：10s / 30s / 60s / 120s / 300s，最多重试 5 次后转 SENT 标记失败（避免无限重试）。
     */
    @Scheduled(fixedDelay = 30000)
    public void relayPendingMessages() {
        var pending = outboxMapper.selectPendingBatch(LocalDateTime.now(), 100);
        if (pending.isEmpty()) {
            return;
        }
        log.info("Outbox 兜底任务扫描到 {} 条待重投消息", pending.size());
        for (MessageOutbox msg : pending) {
            if (msg.getRetryCount() >= 5) {
                log.error("Outbox 消息重试 5 次仍失败，标记为 DEAD: id={}, aggregateId={}",
                        msg.getId(), msg.getAggregateId());
                msg.setStatus("DEAD");
                msg.setUpdatedAt(LocalDateTime.now());
                outboxMapper.updateById(msg);
                continue;
            }
            try {
                trySend(msg);
            } catch (Exception e) {
                int newRetry = msg.getRetryCount() + 1;
                msg.setRetryCount(newRetry);
                msg.setNextRetryAt(LocalDateTime.now().plusSeconds(backoffSeconds(newRetry)));
                msg.setUpdatedAt(LocalDateTime.now());
                outboxMapper.updateById(msg);
                log.warn("Outbox 重投失败，下次重试: id={}, retry={}, nextRetryAt={}",
                        msg.getId(), newRetry, msg.getNextRetryAt());
            }
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
