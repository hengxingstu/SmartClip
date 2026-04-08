package com.smartclip.clip.service;

import org.springframework.stereotype.Service;

@Service
/**
 * 剪贴板内容展示文本生成服务，负责从完整内容中生成标题和列表预览。
 */
public class ClipPreviewService {

    private static final int TITLE_MAX_LENGTH = 80;
    private static final int PREVIEW_MAX_LENGTH = 240;

    /**
     * 生成列表和详情中使用的短标题。
     */
    public String buildTitle(String content) {
        String normalized = normalizeForDisplay(content);
        if (normalized.isBlank()) {
            return "Untitled clip";
        }
        return abbreviate(normalized, TITLE_MAX_LENGTH);
    }

    /**
     * 生成列表页预览文本，避免完整内容撑开表格。
     */
    public String buildPreview(String content) {
        String normalized = normalizeForDisplay(content);
        return abbreviate(normalized, PREVIEW_MAX_LENGTH);
    }

    private String normalizeForDisplay(String content) {
        if (content == null) {
            return "";
        }
        return content.strip().replaceAll("\\s+", " ");
    }

    private String abbreviate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
