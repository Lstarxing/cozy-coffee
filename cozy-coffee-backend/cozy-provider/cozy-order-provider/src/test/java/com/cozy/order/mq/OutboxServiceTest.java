package com.cozy.order.mq;

import com.cozy.order.entity.MessageOutbox;
import com.cozy.order.mapper.MessageOutboxMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock private MessageOutboxMapper outboxMapper;
    @Mock private RocketMQTemplate rocketMQTemplate;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @InjectMocks private OutboxService outboxService;

    private MessageOutbox pendingMsg(int retryCount) {
        MessageOutbox m = new MessageOutbox();
        m.setId(1L);
        m.setAggregateId(100L);
        m.setTopic("cozy_order_events");
        m.setTag("ORDER_CANCELLED");
        m.setPayload("{}");
        m.setStatus("PENDING");
        m.setRetryCount(retryCount);
        m.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
        return m;
    }

    @Test
    void relayPendingMessages_sendFails_incrementsRetryAndBacksOff() {
        MessageOutbox msg = pendingMsg(0);
        when(outboxMapper.selectPendingBatch(any(), anyInt())).thenReturn(List.of(msg));
        doThrow(new RuntimeException("broker down"))
                .when(rocketMQTemplate).syncSend(anyString(), any(org.springframework.messaging.Message.class));

        outboxService.relayPendingMessages();

        assertEquals(1, msg.getRetryCount());
        assertEquals("PENDING", msg.getStatus());
        verify(outboxMapper).updateById(msg);
    }

    @Test
    void relayPendingMessages_retryCount5_marksDead() {
        MessageOutbox msg = pendingMsg(5);
        when(outboxMapper.selectPendingBatch(any(), anyInt())).thenReturn(List.of(msg));

        outboxService.relayPendingMessages();

        assertEquals("DEAD", msg.getStatus());
        verify(outboxMapper).updateById(msg);
    }
}
