package com.smartclip.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * 应用设置响应 DTO，对前端暴露 MVP 阶段可配置项。
 */
public class AppSettingResponse {

    private Boolean listenerEnabled;
    private Integer pollIntervalMs;
    private Integer minTextLength;
    private Boolean ignoreSensitiveEnabled;
}
