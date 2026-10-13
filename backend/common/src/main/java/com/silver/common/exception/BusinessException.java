package com.silver.common.exception;

import lombok.Getter;

/** 业务失败只抛异常，响应由 GlobalExceptionHandler 统一生成。 */
@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        if (code == null || code < 400 || code > 599) {
            throw new IllegalArgumentException("业务异常状态码必须在 400 到 599 之间");
        }
        this.code = code;
    }
}
