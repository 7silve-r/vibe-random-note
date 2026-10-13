package com.silver.handler;

import com.silver.common.Result;
import com.silver.common.exception.BusinessException;
import com.silver.storage.exception.FileStorageException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
        org.springframework.dao.DataIntegrityViolationException.class,
        java.sql.SQLIntegrityConstraintViolationException.class
    })
    public ResponseEntity<Result<Void>> handleConflict(Exception ex) {
        log.warn("数据约束冲突", ex);
        return ResponseEntity.status(409).body(error(409, "数据重复或仍被引用，请刷新后重试"));
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<Result<Void>> handleDatabase(Exception ex) {
        log.error("数据库访问失败", ex);
        return ResponseEntity.status(503).body(error(503, "数据服务暂不可用，请稍后重试"));
    }

    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<Result<Void>> handleFile(Exception ex) {
        log.error("文件读写失败", ex);
        return ResponseEntity.status(500).body(error(500, "文件处理失败，请稍后重试"));
    }

    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    public ResponseEntity<Result<Void>> handleMultipart(Exception ex) {
        return ResponseEntity.badRequest().body(error(400, "上传请求格式错误，请重新选择文件"));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getCode()).body(error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Result<Void>> handleStorage(FileStorageException ex) {
        if (ex.getCode() >= 500) log.error("文件存储操作失败", ex);
        return ResponseEntity.status(ex.getCode()).body(error(ex.getCode(), ex.getMessage()));
    }

    // Spring MVC 将参数绑定、JSON 解析、请求方式和状态异常等交给这里处理。
    // 父类要求 ResponseEntity<Object>；Java 泛型不允许改成 ResponseEntity<Result<Void>>。
    // 仅这个框架适配方法保留签名，实际 body 始终为 Result<Void>。
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            @NonNull Exception ex,
            Object body,
            @NonNull HttpHeaders headers,
            HttpStatusCode status,
            @NonNull WebRequest request) {
        String message =
                switch (status.value()) {
                    case 400 -> "请求参数缺失或格式错误";
                    case 401 -> "请先登录再操作";
                    case 403 -> "没有权限执行此操作";
                    case 404 -> "请求的资源不存在";
                    case 405 -> "不支持此请求方式";
                    case 413 -> "上传文件过大";
                    case 415 -> "不支持此请求内容类型";
                    default -> "请求处理失败";
                };
        if (ex instanceof HttpMessageNotReadableException) {
            message = "请求体格式错误，请检查 JSON";
        } else if (ex instanceof BindException binding) {
            message =
                    binding.getBindingResult().getAllErrors().stream()
                            .map(
                                    error ->
                                            error
                                                                    instanceof
                                                                    org.springframework.validation
                                                                                    .FieldError
                                                                            field
                                                            && field.isBindingFailure()
                                                    ? "请求参数类型错误"
                                                    : error.getDefaultMessage())
                            .filter(text -> text != null && !text.isBlank())
                            .findFirst()
                            .orElse("请求参数校验失败");
        } else if (ex instanceof ResponseStatusException responseStatus
                && status.is4xxClientError()
                && responseStatus.getReason() != null) {
            message = responseStatus.getReason();
        }
        if (status.is5xxServerError()) {
            log.error("请求处理发生服务端异常", ex);
            message = "服务器内部错误，请稍后重试";
        }
        // 保留 Spring 的 HTTP 状态及 Allow 等响应头。
        return super.handleExceptionInternal(
                ex, error(status.value(), message), headers, status, request);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccess(Exception ex) {
        return ResponseEntity.status(403).body(error(403, "没有权限执行此操作"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception ex) {
        log.error("未处理的服务端异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(500, "服务器内部错误，请稍后重试"));
    }

    private Result<Void> error(int code, String message) {
        return Result.error(code, message);
    }
}
