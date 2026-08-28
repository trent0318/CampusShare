package com.campusshare.exception;

import lombok.Getter;

/**
 * 业务异常：由 Service 层抛出，全局异常处理器统一转成错误响应
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
