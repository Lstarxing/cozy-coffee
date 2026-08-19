package com.cozy.order.service;

import com.cozy.common.exception.BusinessException;
import com.cozy.order.entity.PickupCodeCounter;
import com.cozy.order.mapper.PickupCodeCounterMapper;
import com.cozy.order.service.order.PickupCodeService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PickupCodeServiceTest {

    private final PickupCodeCounterMapper counterMapper = mock(PickupCodeCounterMapper.class);
    private final PickupCodeService service = new PickupCodeService(counterMapper);

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 8, 20);

    @Test
    void businessDateIsPreviousDayBefore5am() {
        assertEquals(LocalDate.of(2026, 8, 19),
                service.calculateBusinessDate(LocalDateTime.of(2026, 8, 20, 4, 59)));
    }

    @Test
    void businessDateIsSameDayAtOrAfter5am() {
        assertEquals(BUSINESS_DATE, service.calculateBusinessDate(LocalDateTime.of(2026, 8, 20, 5, 0)));
        assertEquals(BUSINESS_DATE, service.calculateBusinessDate(LocalDateTime.of(2026, 8, 20, 23, 59)));
    }

    @Test
    void firstCodeOfDayStartsAt001() {
        // 首次查无 → insert 后重新查询取锁（生产流程双查询）
        when(counterMapper.selectForUpdate(any(), any()))
                .thenReturn(null)
                .thenReturn(counterWithLastSeq(0));
        LocalDateTime orderTime = LocalDateTime.of(2026, 8, 20, 10, 0);
        assertEquals("001", service.generatePickupCode(1L, orderTime));
        verify(counterMapper).insert(any(PickupCodeCounter.class));
        verify(counterMapper).incrementSeq(1L, BUSINESS_DATE);
    }

    @Test
    void codeIncrementsFromExistingCounter() {
        when(counterMapper.selectForUpdate(1L, BUSINESS_DATE)).thenReturn(counterWithLastSeq(37));
        assertEquals("038", service.generatePickupCode(1L, LocalDateTime.of(2026, 8, 20, 10, 0)));
        verify(counterMapper, never()).insert(any(PickupCodeCounter.class));
        verify(counterMapper).incrementSeq(1L, BUSINESS_DATE);
    }

    @Test
    void codePadsToThreeDigits() {
        when(counterMapper.selectForUpdate(1L, BUSINESS_DATE)).thenReturn(counterWithLastSeq(99));
        assertEquals("100", service.generatePickupCode(1L, LocalDateTime.of(2026, 8, 20, 10, 0)));
    }

    @Test
    void exhaustionThrowsBusinessException() {
        when(counterMapper.selectForUpdate(1L, BUSINESS_DATE)).thenReturn(counterWithLastSeq(999));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.generatePickupCode(1L, LocalDateTime.of(2026, 8, 20, 10, 0)));
        assertTrue(ex.getMessage().contains("PICKUP_CODE_EXHAUSTED"));
    }

    private PickupCodeCounter counterWithLastSeq(int lastSeq) {
        PickupCodeCounter counter = new PickupCodeCounter();
        counter.setStoreId(1L);
        counter.setBusinessDate(BUSINESS_DATE);
        counter.setLastSeq(lastSeq);
        return counter;
    }
}
