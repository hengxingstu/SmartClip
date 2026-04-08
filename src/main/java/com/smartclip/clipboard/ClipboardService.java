package com.smartclip.clipboard;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.smartclip.common.util.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
/**
 * 剪贴板应用服务，封装系统剪贴板读写并记录程序主动写入的内容哈希。
 */
public class ClipboardService {

    private final SystemClipboardAdapter systemClipboardAdapter;
    private final AtomicReference<String> lastProgrammaticHash = new AtomicReference<>();

    /**
     * 读取系统剪贴板文本。
     */
    public Optional<String> readText() {
        return systemClipboardAdapter.readText();
    }

    /**
     * 写入系统剪贴板，并记录该内容由程序主动写入，避免轮询器重复采集。
     */
    public void writeText(String text) {
        systemClipboardAdapter.writeText(text);
        lastProgrammaticHash.set(HashUtils.sha256NormalizedText(text));
    }

    /**
     * 判断当前文本是否为程序刚刚主动写入剪贴板的内容。
     */
    public boolean isProgrammaticText(String text) {
        String hash = HashUtils.sha256NormalizedText(text);
        return hash.equals(lastProgrammaticHash.get());
    }
}
