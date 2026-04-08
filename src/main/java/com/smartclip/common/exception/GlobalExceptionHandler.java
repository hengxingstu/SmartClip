package com.smartclip.common.exception;

import com.smartclip.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
/**
 * 全局异常处理器，将常见业务异常和参数异常转换为统一 API 响应。
 */
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    /**
     * 处理资源不存在异常。
     */
    public ApiResponse<Void> handleNotFound(NotFoundException exception) {
        return ApiResponse.error(exception.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    /**
     * 处理参数校验和非法参数异常。
     */
    public ApiResponse<Void> handleBadRequest(Exception exception) {
        return ApiResponse.error(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    /**
     * 兜底处理未预期异常，避免内部堆栈直接暴露给前端。
     */
    public ApiResponse<Void> handleException(Exception exception) {
        log.error("Unhandled SmartClip error", exception);
        return ApiResponse.error("Internal server error");
    }
}
