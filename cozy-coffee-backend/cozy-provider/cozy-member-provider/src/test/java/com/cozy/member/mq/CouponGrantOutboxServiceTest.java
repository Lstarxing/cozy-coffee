package com.cozy.member.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.member.entity.CouponGrantOutbox;
import com.cozy.member.mapper.CouponGrantOutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.Message;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CouponGrantOutboxServiceTest {

    private CouponGrantOutboxMapper outboxMapper;
    private RocketMQTemplate rocketMQTemplate;
    private CouponGrantOutboxService service;

    @BeforeEach
    void setUp() {
        outboxMapper = mock(CouponGrantOutboxMapper.class);
        rocketMQTemplate = mock(RocketMQTemplate.class);
        service = new CouponGrantOutboxService(outboxMapper, rocketMQTemplate, new ObjectMapper());
    }

    @Test
    void publishWritesPendingRowThenSendsAndMarksSent() {
        service.publish(38L, "BOGO", "signin_7day_9", 0, 40, 30, "signin_7day");

        ArgumentCaptor<CouponGrantOutbox> captor = ArgumentCaptor.forClass(CouponGrantOutbox.class);
        verify(outboxMapper).insert(captor.capture());
        CouponGrantOutbox row = captor.getValue();
        assertEquals("signin_7day_9", row.getUniqueKey());
        assertEquals(38L, row.getUserId());
        assertTrue(row.getPayload().contains("signin_7day_9"), "载荷应含幂等键");

        // 无事务环境 → AfterCommit 立即执行 → 已投递并置 SENT
        verify(rocketMQTemplate).syncSend(
                eq(MqTopics.MEMBER_EVENTS + ":" + MqTags.COUPON_GRANT_REQUESTED), any(Message.class));
        assertEquals("SENT", row.getStatus());
    }

    @Test
    void publishSkipsWhenTheSameGrantIsAlreadyEnqueued() {
        when(outboxMapper.insert(any(CouponGrantOutbox.class))).thenThrow(new DuplicateKeyException("uk_unique_key"));

        service.publish(38L, "BOGO", "signin_7day_9", 0, 40, 30, "signin_7day");

        // 已入队即视为成功，不再重复投递
        verifyNoInteractions(rocketMQTemplate);
    }

    @Test
    void publishLeavesRowPendingWhenSendFails() {
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class)))
                .thenThrow(new RuntimeException("broker down"));

        service.publish(38L, "BOGO", "signin_7day_9", 0, 40, 30, "signin_7day");

        ArgumentCaptor<CouponGrantOutbox> captor = ArgumentCaptor.forClass(CouponGrantOutbox.class);
        verify(outboxMapper).insert(captor.capture());
        // 投递失败不能影响调用方事务，留给 relay 兜底
        assertEquals("PENDING", captor.getValue().getStatus());
    }

    @Test
    void relayResendsPendingRowsAndMarksSent() {
        CouponGrantOutbox pending = pendingRow(1L, 0);
        when(outboxMapper.selectPendingBatch(any(), eq(100))).thenReturn(List.of(pending));

        service.relayPending();

        verify(rocketMQTemplate).syncSend(anyString(), any(Message.class));
        assertEquals("SENT", pending.getStatus());
    }

    @Test
    void relayBacksOffWhenResendFails() {
        CouponGrantOutbox pending = pendingRow(2L, 0);
        when(outboxMapper.selectPendingBatch(any(), eq(100))).thenReturn(List.of(pending));
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class)))
                .thenThrow(new RuntimeException("down"));

        service.relayPending();

        assertEquals("PENDING", pending.getStatus());
        assertEquals(1, pending.getRetryCount());
        assertNotNull(pending.getNextRetryAt());
    }

    @Test
    void relayMarksDeadAfterMaxRetries() {
        CouponGrantOutbox exhausted = pendingRow(3L, 5);
        when(outboxMapper.selectPendingBatch(any(), eq(100))).thenReturn(List.of(exhausted));

        service.relayPending();

        assertEquals("DEAD", exhausted.getStatus());
        verifyNoInteractions(rocketMQTemplate);
    }

    private CouponGrantOutbox pendingRow(Long id, int retryCount) {
        CouponGrantOutbox row = new CouponGrantOutbox();
        row.setId(id);
        row.setUniqueKey("k" + id);
        row.setUserId(38L);
        row.setPayload("{}");
        row.setStatus("PENDING");
        row.setRetryCount(retryCount);
        row.setNextRetryAt(LocalDateTime.now());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        return row;
    }
}
