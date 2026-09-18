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

    /** 线上容器是 Linux，Dubbo 拼接的堆栈用 \n 分隔 —— 不能把堆栈回给客户端。 */
    @Test
    void stripsDubboAppendedStackOnLinuxLineSeparator() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        String wrapped = "密码错误\n\tat com.cozy.user.service.impl.UserServiceImpl.login(UserServiceImpl.java:195)"
                + "\n\tat java.base/java.lang.Thread.run(Unknown Source)";

        Result<?> result = handler.handleRuntimeException(new RuntimeException(wrapped));

        assertEquals("密码错误", result.getMessage());
    }

    /** Windows 开发机的行分隔符是 \r\n，两种都要能清洗。 */
    @Test
    void stripsDubboAppendedStackOnWindowsLineSeparator() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        String wrapped = "密码错误\r\n\tat com.cozy.user.service.impl.UserServiceImpl.login(UserServiceImpl.java:195)";

        Result<?> result = handler.handleRuntimeException(new RuntimeException(wrapped));

        assertEquals("密码错误", result.getMessage());
    }

    @Test
    void stripsExceptionClassPrefixFromDubboMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        String wrapped = "com.cozy.common.exception.BusinessException: 账号不存在\n\tat com.cozy.X.y(X.java:1)";

        Result<?> result = handler.handleRuntimeException(new RuntimeException(wrapped));

        assertEquals("账号不存在", result.getMessage());
    }

    @Test
    void keepsPlainMessageWithoutLineBreakUnchanged() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<?> result = handler.handleRuntimeException(new RuntimeException("服务繁忙，请稍后重试"));

        assertEquals("服务繁忙，请稍后重试", result.getMessage());
    }
}
