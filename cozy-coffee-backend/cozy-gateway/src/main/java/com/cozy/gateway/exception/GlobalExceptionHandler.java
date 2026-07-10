package com.cozy.gateway.exception;

import com.cozy.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器（Phase 7 M7）。
 * 替代各 Controller 中散落的 try/catch，统一转为 Result 响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleUnauthorized(UnauthorizedException e) {
        return Result.unauthorized();
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNotFound(NotFoundException e) {
        return Result.notFound(e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error("参数错误: " + e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error("缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleDuplicateKey(DuplicateKeyException e) {
        String msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("uk_phone") || msg.contains("phone")) {
                return Result.fail("该手机号已被其他账号绑定");
            }
            if (msg.contains("uk_email") || msg.contains("email")) {
                return Result.fail("该邮箱已被其他账号绑定");
            }
        }
        return Result.fail("该账号信息已存在，请核对后重试");
    }

    @ExceptionHandler(StorageException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleStorageException(StorageException e) {
        log.error("文件上传失败", e);
        return Result.fail("文件上传失败: " + e.getMessage());
    }

    /** Dubbo RPC 调用失败（超时、无服务提供者、连接断开等） */
    @ExceptionHandler(RpcException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleRpcException(RpcException e) {
        log.error("Dubbo RPC 调用失败", e);
        return Result.fail("服务繁忙，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleUnknown(Exception e) {
        log.error("未预期异常", e);
        return Result.error("系统繁忙，请稍后重试");
    }
}
