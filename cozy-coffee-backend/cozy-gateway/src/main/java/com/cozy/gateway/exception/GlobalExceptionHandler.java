package com.cozy.gateway.exception;

import com.cozy.common.exception.BusinessException;
import com.cozy.common.exception.NotFoundException;
import com.cozy.common.exception.UnauthorizedException;
import com.cozy.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器（Phase 7 M7）。
 * 替代各 Controller 中散落的 try/catch，统一转为 Result 响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** JSR-303 @RequestBody 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", errors);
        return Result.fail(errors);
    }

    /** JSR-303 @RequestParam / @PathVariable 校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolation(ConstraintViolationException e) {
        String errors = e.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数约束违反: {}", errors);
        return Result.fail(errors);
    }

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

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessException(BusinessException e) {
        String msg = cleanDubboMessage(e.getMessage());
        log.warn("业务异常: {}", msg);
        return Result.businessFail(e.getCode().name(), msg, e.isRetryable());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleRuntimeException(RuntimeException e) {
        BusinessException business = findBusinessException(e);
        if (business != null) {
            return Result.businessFail(business.getCode().name(), cleanDubboMessage(business.getMessage()),
                    business.isRetryable());
        }
        String msg = cleanDubboMessage(e.getMessage());
        log.warn("业务异常: {}", msg);
        return Result.fail(msg);
    }

    private BusinessException findBusinessException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BusinessException business) return business;
            if (current.getCause() == current) break;
            current = current.getCause();
        }
        return null;
    }

    /**
     * Dubbo ExceptionFilter 会把「异常信息 + 完整堆栈」拼成一个字符串回传，必须在网关侧清洗，
     * 否则整段堆栈会当作 message 返回给客户端（线上实测 5KB）。
     * 换行符随运行平台变化——Linux 容器是 \n、Windows 开发机是 \r\n，故按任意换行切分。
     */
    private String cleanDubboMessage(String msg) {
        if (msg == null) return null;
        // 只取第一行，丢掉 Dubbo 拼接进来的堆栈
        int cut = msg.length();
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (c == '\n' || c == '\r') {
                cut = i;
                break;
            }
        }
        String firstLine = msg.substring(0, cut).trim();
        // 去类名前缀 "com.xxx.BusinessException: "
        if (firstLine.contains("BusinessException: ")) {
            int colonIdx = firstLine.lastIndexOf("BusinessException: ");
            firstLine = firstLine.substring(colonIdx + "BusinessException: ".length());
        }
        return firstLine;
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
