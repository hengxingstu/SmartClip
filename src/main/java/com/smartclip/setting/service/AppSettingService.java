package com.smartclip.setting.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartclip.setting.dto.AppSettingResponse;
import com.smartclip.setting.dto.AppSettingUpdateRequest;
import com.smartclip.setting.entity.AppSetting;
import com.smartclip.setting.mapper.AppSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
/**
 * 应用设置服务，负责读取和更新剪贴板监听相关的本地配置。
 */
public class AppSettingService {

    public static final String LISTENER_ENABLED = "clipboard.listener.enabled";
    public static final String POLL_INTERVAL_MS = "clipboard.poll.interval.ms";
    public static final String MIN_TEXT_LENGTH = "clipboard.min.text.length";
    public static final String IGNORE_SENSITIVE_ENABLED = "clipboard.ignore.sensitive.enabled";

    private static final int DEFAULT_POLL_INTERVAL_MS = 1000;
    private static final int DEFAULT_MIN_TEXT_LENGTH = 3;

    private final AppSettingMapper appSettingMapper;

    /**
     * 聚合返回前端设置页需要展示的所有配置项。
     */
    public AppSettingResponse getSettings() {
        Map<String, AppSetting> settings = appSettingMapper.selectList(Wrappers.emptyWrapper())
                .stream()
                .collect(Collectors.toMap(AppSetting::getSettingKey, Function.identity(), (left, right) -> left));

        return new AppSettingResponse(
                getBoolean(settings, LISTENER_ENABLED, true),
                getInteger(settings, POLL_INTERVAL_MS, DEFAULT_POLL_INTERVAL_MS),
                getInteger(settings, MIN_TEXT_LENGTH, DEFAULT_MIN_TEXT_LENGTH),
                getBoolean(settings, IGNORE_SENSITIVE_ENABLED, false)
        );
    }

    /**
     * 判断剪贴板监听是否启用，缺失配置时使用默认值 true。
     */
    public boolean isListenerEnabled() {
        return getBoolean(LISTENER_ENABLED, true);
    }

    /**
     * 获取轮询间隔毫秒数，缺失或非法配置时回退到默认值。
     */
    public int getPollIntervalMs() {
        return getInteger(POLL_INTERVAL_MS, DEFAULT_POLL_INTERVAL_MS);
    }

    /**
     * 获取最短保存文本长度，缺失或非法配置时回退到默认值。
     */
    public int getMinTextLength() {
        return getInteger(MIN_TEXT_LENGTH, DEFAULT_MIN_TEXT_LENGTH);
    }

    /**
     * 判断是否忽略敏感内容。
     */
    public boolean isIgnoreSensitiveEnabled() {
        return getBoolean(IGNORE_SENSITIVE_ENABLED, false);
    }

    @Transactional
    /**
     * 全量更新 MVP 设置项，并返回数据库中的最新值。
     */
    public AppSettingResponse updateSettings(AppSettingUpdateRequest request) {
        updateSetting(LISTENER_ENABLED, request.getListenerEnabled().toString(), "BOOLEAN", "Whether clipboard listener is enabled");
        updateSetting(POLL_INTERVAL_MS, request.getPollIntervalMs().toString(), "INTEGER", "Clipboard polling interval in milliseconds");
        updateSetting(MIN_TEXT_LENGTH, request.getMinTextLength().toString(), "INTEGER", "Minimum trimmed text length to save");
        updateSetting(IGNORE_SENSITIVE_ENABLED, request.getIgnoreSensitiveEnabled().toString(), "BOOLEAN", "Whether sensitive content should be ignored");
        return getSettings();
    }

    /**
     * 读取布尔型设置，适合调度器等高频调用点使用。
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        AppSetting setting = findByKey(key);
        if (setting == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(setting.getSettingValue());
    }

    /**
     * 读取整型设置，并在配置不存在或格式错误时使用默认值。
     */
    public int getInteger(String key, int defaultValue) {
        AppSetting setting = findByKey(key);
        if (setting == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(setting.getSettingValue());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    /**
     * 按设置键查询单个设置项。
     */
    private AppSetting findByKey(String key) {
        return appSettingMapper.selectOne(Wrappers.<AppSetting>lambdaQuery()
                .eq(AppSetting::getSettingKey, key)
                .last("LIMIT 1"));
    }

    private boolean getBoolean(Map<String, AppSetting> settings, String key, boolean defaultValue) {
        AppSetting setting = settings.get(key);
        return setting == null ? defaultValue : Boolean.parseBoolean(setting.getSettingValue());
    }

    private int getInteger(Map<String, AppSetting> settings, String key, int defaultValue) {
        AppSetting setting = settings.get(key);
        if (setting == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(setting.getSettingValue());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    /**
     * 以 upsert 方式更新单个设置项，缺失时自动创建。
     */
    private void updateSetting(String key, String value, String valueType, String description) {
        AppSetting existing = findByKey(key);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            AppSetting setting = new AppSetting();
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            setting.setValueType(valueType);
            setting.setDescription(description);
            setting.setUpdatedAt(now);
            appSettingMapper.insert(setting);
            return;
        }

        existing.setSettingValue(value);
        existing.setValueType(valueType);
        existing.setDescription(description);
        existing.setUpdatedAt(now);
        appSettingMapper.updateById(existing);
    }
}
