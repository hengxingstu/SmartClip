package com.smartclip.setting.controller;

import com.smartclip.common.api.ApiResponse;
import com.smartclip.setting.dto.AppSettingResponse;
import com.smartclip.setting.dto.AppSettingUpdateRequest;
import com.smartclip.setting.service.AppSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settings")
/**
 * 应用设置 REST 控制器，暴露监听开关、轮询间隔等 MVP 配置。
 */
public class AppSettingController {

    private final AppSettingService appSettingService;

    @GetMapping
    /**
     * 读取当前本地应用设置。
     */
    public ApiResponse<AppSettingResponse> getSettings() {
        return ApiResponse.ok(appSettingService.getSettings());
    }

    @PutMapping
    /**
     * 全量更新 MVP 设置项，并返回更新后的设置。
     */
    public ApiResponse<AppSettingResponse> updateSettings(@Valid @RequestBody AppSettingUpdateRequest request) {
        return ApiResponse.ok(appSettingService.updateSettings(request));
    }
}
