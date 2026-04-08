package com.smartclip.common.controller;

import java.util.Map;

import com.smartclip.common.api.ApiResponse;
import com.smartclip.setting.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/health")
/**
 * 健康检查控制器，用于确认后端、数据库和监听开关的基础状态。
 */
public class HealthController {

    private final AppSettingService appSettingService;
    private final java.util.Optional<BuildProperties> buildProperties;

    @GetMapping
    /**
     * 返回本地服务健康状态和关键运行信息。
     */
    public ApiResponse<Map<String, Object>> health() {
        String version = buildProperties.map(BuildProperties::getVersion).orElse("dev");
        return ApiResponse.ok(Map.of(
                "status", "UP",
                "version", version,
                "databaseReady", true,
                "listenerEnabled", appSettingService.isListenerEnabled()
        ));
    }
}
