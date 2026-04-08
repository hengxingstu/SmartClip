package com.smartclip.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * 统一 API 响应包装，保持前后端成功标记、数据和错误信息格式一致。
 */
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;

    /**
     * 构造成功响应。
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * 构造失败响应，通常由全局异常处理器使用。
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
