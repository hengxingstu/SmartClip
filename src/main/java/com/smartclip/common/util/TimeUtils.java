package com.smartclip.common.util;

import java.time.LocalDateTime;

/**
 * 时间工具类，预留统一时间获取入口。
 */
public final class TimeUtils {

    private TimeUtils() {
    }

    /**
     * 返回当前本地时间。
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
