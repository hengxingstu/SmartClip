package com.smartclip.clip.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 内容敏感级别枚举，MVP 阶段用于标记可疑密钥、口令等文本。
 */
public enum SensitivityLevel {
    NORMAL("NORMAL"),
    SENSITIVE("SENSITIVE");

    @EnumValue
    @JsonValue
    private final String code;

    SensitivityLevel(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
