package com.cozy.common.exception;

/**
 * 业务逻辑异常 — 由 GlobalExceptionHandler 统一转为 HTTP 200 + Result.fail(message)。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
