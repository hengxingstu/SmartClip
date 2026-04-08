package com.smartclip.common.exception;

/**
 * 资源不存在异常，用于详情、复制、删除等按 ID 查询的业务场景。
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
