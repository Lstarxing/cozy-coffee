package com.cozy.order.mq;

import com.cozy.common.exception.BusinessException;
import com.cozy.order.dto.response.OrderOutboxDeadLetterDTO;
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
import java.util.List;

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

            // 事务提交后异步投递，避免 sendSync 失败导致业务事务回滚
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (!trySend(msg)) {
                        // 首次直投失败也持久化错误原因，后续进入 DEAD 时管理端可定位根因。
                        msg.setUpdatedAt(LocalDateTime.now());
                        outboxMapper.updateById(msg);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Outbox 写入失败: aggregateId={}, type={}", aggregateId, messageType, e);
            throw new RuntimeException("Outbox 写入失败", e);
        }
    }

    private boolean trySend(MessageOutbox msg) {
        String destination = msg.getTopic() + ":" + msg.getTag();
        try {
            rocketMQTemplate.syncSend(destination,
                    MessageBuilder.withPayload(msg.getPayload())
                            .setHeader("KEYS", String.valueOf(msg.getAggregateId()))
                            .setHeader("OUTBOX_ID", String.valueOf(msg.getId()))
                            .build());
            msg.setStatus("SENT");
            msg.setLastError(null);
            msg.setUpdatedAt(LocalDateTime.now());
            outboxMapper.markSent(msg.getId(), msg.getUpdatedAt());
            log.info("Outbox 消息投递成功: id={}, aggregateId={}", msg.getId(), msg.getAggregateId());
            return true;
        } catch (Exception e) {
            msg.setLastError(truncateError(e));
            // 返回 false，由调用方累计重试（不能吞掉失败，否则重试计数不增长）
            log.warn("Outbox 投递失败，等待重试: id={}, aggregateId={}, error={}",
                    msg.getId(), msg.getAggregateId(), e.getMessage());
            return false;
        }
    }

    /**
     * 兜底任务：每 30s 扫描 PENDING 且 next_retry_at <= now 的消息重投。
     * 指数退避：10s / 30s / 60s / 120s / 300s，最多重试 5 次后转 DEAD（等待告警与人工恢复）。
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
            if (!trySend(msg)) {
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

    public List<OrderOutboxDeadLetterDTO> listDeadMessages(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return outboxMapper.selectDeadBatch(safeLimit).stream().map(this::toDeadLetterDTO).toList();
    }

    public long countDeadMessages() {
        return outboxMapper.countDead();
    }

    public void retryDeadMessage(Long id, Long operatorId) {
        if (id == null) {
            throw new BusinessException("DEAD 消息ID不能为空");
        }
        if (operatorId == null) {
            throw new BusinessException("人工重试操作人不能为空");
        }
        if (outboxMapper.retryDead(id, operatorId, LocalDateTime.now()) == 0) {
            throw new BusinessException("消息不存在或已不处于 DEAD 状态");
        }
        log.warn("订单 outbox 已人工恢复为 PENDING: id={}, operatorId={}", id, operatorId);
    }

    /** 周期告警入口：日志平台应对该 ERROR 文案配置通知规则。 */
    @Scheduled(fixedDelayString = "${cozy.order.outbox-dead-alert-delay-ms:300000}")
    public void alertDeadMessages() {
        long deadCount = outboxMapper.countDead();
        if (deadCount > 0) {
            log.error("CONSISTENCY_ALERT order_outbox_dead_count={}; 请在管理端检查并人工重试", deadCount);
        }
    }

    private OrderOutboxDeadLetterDTO toDeadLetterDTO(MessageOutbox msg) {
        OrderOutboxDeadLetterDTO dto = new OrderOutboxDeadLetterDTO();
        dto.setId(msg.getId());
        dto.setAggregateId(msg.getAggregateId());
        dto.setMessageType(msg.getMessageType());
        dto.setTopic(msg.getTopic());
        dto.setTag(msg.getTag());
        dto.setStatus(msg.getStatus());
        dto.setRetryCount(msg.getRetryCount());
        dto.setManualRetryCount(msg.getManualRetryCount());
        dto.setLastError(msg.getLastError());
        dto.setNextRetryAt(msg.getNextRetryAt());
        dto.setLastManualRetryAt(msg.getLastManualRetryAt());
        dto.setLastManualRetryBy(msg.getLastManualRetryBy());
        dto.setCreatedAt(msg.getCreatedAt());
        dto.setUpdatedAt(msg.getUpdatedAt());
        return dto;
    }

    private int normalizeLimit(Integer limit) {
        return limit == null ? 100 : Math.max(1, Math.min(limit, 200));
    }

    private String truncateError(Exception e) {
        String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return error.length() <= 500 ? error : error.substring(0, 500);
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
