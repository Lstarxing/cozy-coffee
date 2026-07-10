package com.cozy.gateway.exception;

/**
 * 未登录异常 — 由 GlobalExceptionHandler 统一转为 401。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("用户未登录");
    }
}
