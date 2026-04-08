package com.smartclip.setting.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
/**
 * 应用设置更新请求 DTO，包含设置页全量提交的配置项和基础校验。
 */
public class AppSettingUpdateRequest {

    @NotNull
    private Boolean listenerEnabled;

    @NotNull
    @Min(300)
    private Integer pollIntervalMs;

    @NotNull
    @Min(1)
    private Integer minTextLength;

    @NotNull
    private Boolean ignoreSensitiveEnabled;
}
