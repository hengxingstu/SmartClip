package com.smartclip.clip.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 剪贴板文本的基础类型枚举，用于列表筛选和详情展示。
 */
public enum ClipType {
    URL("URL"),
    JSON("JSON"),
    SQL("SQL"),
    COMMAND("COMMAND"),
    JAVA_EXCEPTION_LOG("JAVA_EXCEPTION_LOG"),
    FILE_PATH("FILE_PATH"),
    CODE("CODE"),
    TEXT("TEXT");

    @EnumValue
    @JsonValue
    private final String code;

    ClipType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
