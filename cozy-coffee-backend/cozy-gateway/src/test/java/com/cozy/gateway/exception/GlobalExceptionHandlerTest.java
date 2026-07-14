package com.cozy.gateway.exception;

import com.cozy.common.exception.BusinessErrorCode;
import com.cozy.common.exception.BusinessException;
import com.cozy.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    @Test
    void returnsStableCheckoutErrorShape() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<?> result = handler.handleBusinessException(
                new BusinessException(BusinessErrorCode.PREVIEW_EXPIRED, "preview expired"));

        assertFalse(result.isSuccess());
        assertEquals("PREVIEW_EXPIRED", result.getErrorCode());
        assertEquals("preview expired", result.getMessage());
        assertTrue(result.getRetryable());
    }
}
