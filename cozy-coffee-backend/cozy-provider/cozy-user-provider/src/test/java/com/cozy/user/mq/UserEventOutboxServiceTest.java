package com.cozy.user.mq;

import com.cozy.common.mq.MqTags;
import com.cozy.common.mq.MqTopics;
import com.cozy.common.mq.UserCreatedEvent;
import com.cozy.user.entity.UserEventOutbox;
import com.cozy.user.mapper.UserEventOutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.Message;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserEventOutboxServiceTest {

    private UserEventOutboxMapper outboxMapper;
    private RocketMQTemplate rocketMQTemplate;
    private SimpleMeterRegistry registry;
    private UserEventOutboxService service;

    @BeforeEach
    void setUp() {
        outboxMapper = mock(UserEventOutboxMapper.class);
        rocketMQTemplate = mock(RocketMQTemplate.class);
        registry = new SimpleMeterRegistry();
        service = new UserEventOutboxService(outboxMapper, rocketMQTemplate,
                new ObjectMapper().findAndRegisterModules(), registry);
    }

    @AfterEach
    void cleanupSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishPersistsBeforeCommitAndSendsToRowTagAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(38L).uniqueKey("user_created_38").occurredAt(LocalDateTime.now()).build();

        service.publish(MqTags.USER_CREATED, event.getUserId(), event);

        ArgumentCaptor<UserEventOutbox> captor = ArgumentCaptor.forClass(UserEventOutbox.class);
        verify(outboxMapper).insert(captor.capture());
        UserEventOutbox row = captor.getValue();
        assertEquals(MqTags.USER_CREATED, row.getTag());
        assertEquals("user_created_38", row.getUniqueKey());
        assertEquals("PENDING", row.getStatus());
        assertTrue(row.getPayload().contains("occurredAt"));
        verify(rocketMQTemplate, never()).syncSend(anyString(), any(Message.class));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(rocketMQTemplate).syncSend(eq(MqTopics.USER_EVENTS + ":" + MqTags.USER_CREATED),
                any(Message.class));
        assertEquals("SENT", row.getStatus());
    }

    @Test
    void duplicateTagAndBusinessKeyIsAlreadyEnqueued() {
        when(outboxMapper.insert(any(UserEventOutbox.class)))
                .thenThrow(new DuplicateKeyException("uk_tag_unique_key"));

        service.publish(MqTags.USER_CREATED, 38L,
                UserCreatedEvent.builder().userId(38L).uniqueKey("user_created_38")
                        .occurredAt(LocalDateTime.now()).build());

        verifyNoInteractions(rocketMQTemplate);
    }

    @Test
    void rejectsUnknownTagBeforeWriting() {
        assertThrows(IllegalArgumentException.class,
                () -> service.publish("arbitrary", 38L,
                        UserCreatedEvent.builder().userId(38L).uniqueKey("k1")
                                .occurredAt(LocalDateTime.now()).build()));
        verifyNoInteractions(outboxMapper, rocketMQTemplate);
    }

    @Test
    void rejectsPayloadWhoseTypeDoesNotMatchTag() {
        assertThrows(IllegalArgumentException.class,
                () -> service.publish(MqTags.BIRTHDAY_SET, 38L,
                        UserCreatedEvent.builder().userId(38L).uniqueKey("user_created_38")
                                .occurredAt(LocalDateTime.now()).build()));
        verifyNoInteractions(outboxMapper, rocketMQTemplate);
    }

    @Test
    void relayUsesPersistedTagAndBacksOffAfterFailure() {
        UserEventOutbox row = pendingRow(1L, MqTags.BIRTHDAY_SET, 0);
        when(outboxMapper.selectPendingBatch(any(), eq(100))).thenReturn(List.of(row));
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class)))
                .thenThrow(new RuntimeException("broker down"));

        service.relayPending();

        verify(rocketMQTemplate).syncSend(eq(MqTopics.USER_EVENTS + ":" + MqTags.BIRTHDAY_SET),
                any(Message.class));
        assertEquals(1, row.getRetryCount());
        assertEquals("PENDING", row.getStatus());
        assertNotNull(row.getNextRetryAt());
    }

    @Test
    void relayMarksExhaustedRowsDeadWithoutSending() {
        UserEventOutbox row = pendingRow(2L, MqTags.PROFILE_COMPLETED, 5);
        when(outboxMapper.selectPendingBatch(any(), eq(100))).thenReturn(List.of(row));

        service.relayPending();

        assertEquals("DEAD", row.getStatus());
        verifyNoInteractions(rocketMQTemplate);
    }

    @Test
    void exposesPendingDeadAndOldestAgeGauges() {
        service.registerGauges();
        when(outboxMapper.countByStatus("PENDING")).thenReturn(2L);
        when(outboxMapper.countByStatus("DEAD")).thenReturn(1L);
        when(outboxMapper.oldestPendingCreatedAt()).thenReturn(LocalDateTime.now().minusSeconds(90));
        when(outboxMapper.selectPendingBatch(any(), eq(100))).thenReturn(List.of());

        service.relayPending();

        assertEquals(2.0, registry.get("cozy.user.event_outbox.pending").gauge().value());
        assertEquals(1.0, registry.get("cozy.user.event_outbox.dead").gauge().value());
        assertTrue(registry.get("cozy.user.event_outbox.oldest_pending_age_seconds").gauge().value() >= 85);
    }

    private UserEventOutbox pendingRow(Long id, String tag, int retryCount) {
        UserEventOutbox row = new UserEventOutbox();
        row.setId(id);
        row.setTag(tag);
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
