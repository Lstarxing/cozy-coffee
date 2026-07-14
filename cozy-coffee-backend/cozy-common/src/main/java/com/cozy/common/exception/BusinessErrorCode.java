package com.cozy.common.exception;

/** Stable machine-readable business codes for checkout clients. */
public enum BusinessErrorCode {
    BUSINESS_ERROR(false),
    VALIDATION_ERROR(false),
    STORE_CLOSED(true),
    ITEM_OFFLINE(false),
    ITEM_CHANGED(true),
    COUPON_EXPIRED(false),
    PREVIEW_EXPIRED(true),
    IDEMPOTENCY_KEY_INVALID(false),
    ORDER_CREATE_FAILED(true);

    private final boolean retryable;

    BusinessErrorCode(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
