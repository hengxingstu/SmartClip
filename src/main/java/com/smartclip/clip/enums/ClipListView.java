package com.smartclip.clip.enums;

import java.util.Locale;

/**
 * 剪贴板列表视图枚举，用于区分普通历史、收藏、高频和忽略记录查询。
 */
public enum ClipListView {
    HISTORY,
    FAVORITES,
    FREQUENT,
    IGNORED;

    public static ClipListView fromRequestValue(String value) {
        return value == null ? null : valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
