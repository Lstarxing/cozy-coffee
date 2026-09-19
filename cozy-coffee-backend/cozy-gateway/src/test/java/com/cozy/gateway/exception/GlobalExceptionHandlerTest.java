package com.cozy.gateway.exception;

import com.cozy.common.exception.BusinessErrorCode;
import com.cozy.common.exception.BusinessException;
import com.cozy.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsStableCheckoutErrorShape() {
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
        Result<?> result = handler.handleBusinessException(new BusinessException(
                "密码错误\n\tat com.cozy.user.service.impl.UserServiceImpl.login(UserServiceImpl.java:195)"
                        + "\n\tat java.base/java.lang.Thread.run(Unknown Source)"));

        assertEquals("密码错误", result.getMessage());
    }

    /** Windows 开发机的行分隔符是 \r\n，两种都要能清洗。 */
    @Test
    void stripsDubboAppendedStackOnWindowsLineSeparator() {
        Result<?> result = handler.handleBusinessException(new BusinessException(
                "密码错误\r\n\tat com.cozy.user.service.impl.UserServiceImpl.login(UserServiceImpl.java:195)"));

        assertEquals("密码错误", result.getMessage());
    }

    @Test
    void stripsExceptionClassPrefixFromDubboMessage() {
        Result<?> result = handler.handleBusinessException(new BusinessException(
                "com.cozy.common.exception.BusinessException: 账号不存在\n\tat com.cozy.X.y(X.java:1)"));

        assertEquals("账号不存在", result.getMessage());
    }

    @Test
    void keepsPlainMessageWithoutLineBreakUnchanged() {
        Result<?> result = handler.handleBusinessException(new BusinessException("账号不存在"));

        assertEquals("账号不存在", result.getMessage());
    }

    /** 业务异常被别的 RuntimeException 包住时，仍按业务失败（HTTP 200 + errorCode）返回。 */
    @Test
    void keepsBusinessFailureWhenWrappedInAnotherRuntimeException() {
        ResponseEntity<Result<?>> response = handler.handleRuntimeException(
                new RuntimeException("wrapper", new BusinessException(BusinessErrorCode.STORE_CLOSED, "门店已打烊")));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Result<?> body = response.getBody();
        assertNotNull(body);
        assertEquals("STORE_CLOSED", body.getErrorCode());
        assertEquals("门店已打烊", body.getMessage());
        assertTrue(body.getRetryable());
    }

    /** 未预期缺陷：状态码 500，且不把异常内容透给客户端。 */
    @Test
    void hidesUnexpectedExceptionDetailsBehindGenericMessage() {
        ResponseEntity<Result<?>> response = handler.handleRuntimeException(new RuntimeException(
                "java.lang.NullPointerException: Cannot invoke \"CoffeeProductDTO.getServingConfig()\""));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Result<?> body = response.getBody();
        assertNotNull(body);
        assertEquals("系统繁忙，请稍后重试", body.getMessage());
    }

    /** RPC 故障属可重试的临时故障，不能伪装成业务失败。 */
    @Test
    void mapsRpcFailureToRetryableServiceUnavailable() {
        Result<?> result = handler.handleRpcException(new RpcException("connect failed"));

        assertEquals(503, result.getCode());
        assertEquals("服务繁忙，请稍后重试", result.getMessage());
        assertTrue(result.getRetryable());
    }

    // ==================== 参数校验类：统一带 VALIDATION_ERROR ====================

    /** 校验类失败统一形状：code 400 + errorCode=VALIDATION_ERROR + retryable=false（移动端据此归为 ValidationError） */
    private void assertValidationShape(Result<?> result) {
        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
        assertEquals(BusinessErrorCode.VALIDATION_ERROR.name(), result.getErrorCode());
        assertEquals(Boolean.FALSE, result.getRetryable());
    }

    @Test
    void bodyValidationFailureCarriesValidationErrorCode() throws Exception {
        MethodParameter param = new MethodParameter(String.class.getDeclaredMethod("substring", int.class), 0);
        BindingResult binding = new BeanPropertyBindingResult(new Object(), "req");

        Result<?> result = handler.handleValidation(new MethodArgumentNotValidException(param, binding));

        assertValidationShape(result);
    }

    @Test
    void constraintViolationCarriesValidationErrorCode() {
        Result<?> result = handler.handleConstraintViolation(new ConstraintViolationException(Set.of()));

        assertValidationShape(result);
    }

    @Test
    void missingRequestParameterCarriesValidationErrorCode() {
        Result<?> result = handler.handleMissingParam(new MissingServletRequestParameterException("q", "String"));

        assertValidationShape(result);
        assertTrue(result.getMessage().contains("q"));
    }

    @Test
    void illegalArgumentCarriesValidationErrorCode() {
        Result<?> result = handler.handleIllegalArgument(new IllegalArgumentException("bad arg"));

        assertValidationShape(result);
    }
}
