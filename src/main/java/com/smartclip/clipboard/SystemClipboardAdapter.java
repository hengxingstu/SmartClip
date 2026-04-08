package com.smartclip.clipboard;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
/**
 * 系统剪贴板适配器，隔离 AWT 剪贴板 API 与业务服务。
 */
public class SystemClipboardAdapter {

    /**
     * 尝试读取系统剪贴板中的文本内容；剪贴板不可用或非文本时返回空。
     */
    public Optional<String> readText() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return Optional.empty();
            }
            Object data = clipboard.getData(DataFlavor.stringFlavor);
            return data instanceof String text ? Optional.of(text) : Optional.empty();
        } catch (HeadlessException | IllegalStateException | UnsupportedFlavorException | IOException exception) {
            log.debug("Clipboard text read skipped: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 将指定文本写入系统剪贴板。
     */
    public void writeText(String text) {
        try {
            StringSelection selection = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        } catch (HeadlessException | IllegalStateException exception) {
            throw new IllegalStateException("Cannot write to system clipboard", exception);
        }
    }
}
