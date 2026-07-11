package com.cozy.common.exception;

/**
 * 资源未找到异常 — 由 GlobalExceptionHandler 统一转为 404。
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
