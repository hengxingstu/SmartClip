package com.smartclip.clipboard;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.smartclip.clip.service.ClipItemService;
import com.smartclip.common.util.HashUtils;
import com.smartclip.setting.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * 剪贴板轮询调度器，按设置读取系统剪贴板并触发文本采集。
 */
public class ClipboardMonitorScheduler {

    private final ClipboardService clipboardService;
    private final ClipItemService clipItemService;
    private final AppSettingService appSettingService;
    private final AtomicReference<String> lastObservedHash = new AtomicReference<>();

    private volatile long lastPollAt;

    @Scheduled(fixedDelayString = "${smartclip.scheduler.tick-ms:500}")
    /**
     * 定时检查剪贴板文本变化；通过内存哈希避免同一内容在连续轮询中重复入库。
     */
    public void pollClipboard() {
        if (!appSettingService.isListenerEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        int intervalMs = appSettingService.getPollIntervalMs();
        if (now - lastPollAt < intervalMs) {
            return;
        }
        lastPollAt = now;

        Optional<String> text = clipboardService.readText();
        if (text.isEmpty()) {
            return;
        }

        String content = text.get();
        String currentHash = HashUtils.sha256NormalizedText(content);
        if (currentHash.equals(lastObservedHash.get())) {
            return;
        }
        lastObservedHash.set(currentHash);

        if (clipboardService.isProgrammaticText(content)) {
            return;
        }

        try {
            clipItemService.captureText(content);
        } catch (RuntimeException exception) {
            log.warn("Clipboard capture failed: {}", exception.getMessage());
        }
    }
}
