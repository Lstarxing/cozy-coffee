package com.cozy.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装
 * 
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 状态码
     */
    private int code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /** Stable machine-readable business code. Null for successful responses. */
    private String errorCode;

    /** Whether retrying the same user intent can succeed. */
    private Boolean retryable;

    // 私有构造，使用静态方法创建
    private Result() {
    }

    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return success(data, "操作成功");
    }

    /**
     * 成功响应（带数据和消息）
     */
    public static <T> Result<T> success(T data, String message) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> fail(String message) {
        return fail(400, message);
    }

    /**
     * 失败响应（带状态码）
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        return result;
    }

    public static <T> Result<T> businessFail(String errorCode, String message, boolean retryable) {
        Result<T> result = fail(400, message);
        result.setErrorCode(errorCode);
        result.setRetryable(retryable);
        return result;
    }

    /**
     * 未授权
     */
    public static <T> Result<T> unauthorized() {
        return fail(401, "未授权，请先登录");
    }

    /**
     * 禁止访问
     */
    public static <T> Result<T> forbidden() {
        return fail(403, "无权限访问");
    }

    /**
     * 资源不存在
     */
    public static <T> Result<T> notFound(String message) {
        return fail(404, message);
    }

    /**
     * 服务器错误
     */
    public static <T> Result<T> error(String message) {
        return fail(500, message);
    }
}
