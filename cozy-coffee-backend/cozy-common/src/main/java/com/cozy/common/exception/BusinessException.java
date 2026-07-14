package com.cozy.common.exception;

/**
 * 业务逻辑异常 — 由 GlobalExceptionHandler 统一转为 HTTP 200 + Result.fail(message)。
 */
public class BusinessException extends RuntimeException {

    private final BusinessErrorCode code;
    private final boolean retryable;

    public BusinessException(String message) {
        this(BusinessErrorCode.BUSINESS_ERROR, message);
    }

    public BusinessException(BusinessErrorCode code, String message) {
        this(code, message, code != null && code.isRetryable());
    }

    public BusinessException(BusinessErrorCode code, String message, boolean retryable) {
        super(message);
        this.code = code != null ? code : BusinessErrorCode.BUSINESS_ERROR;
        this.retryable = retryable;
    }

    public BusinessErrorCode getCode() {
        return code;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
