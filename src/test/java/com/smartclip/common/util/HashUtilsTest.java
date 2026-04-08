package com.smartclip.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 哈希工具单元测试，确保去重所依赖的哈希行为稳定。
 */
class HashUtilsTest {

    @Test
    /**
     * 验证不同平台换行符会在哈希前归一化。
     */
    void normalizesLineEndingsBeforeHashing() {
        assertThat(HashUtils.sha256NormalizedText("a\r\nb\r"))
                .isEqualTo(HashUtils.sha256NormalizedText("a\nb\n"));
    }

    @Test
    /**
     * 验证 SHA-256 输出为稳定的 64 位十六进制字符串。
     */
    void returnsStableSha256Hex() {
        assertThat(HashUtils.sha256("SmartClip"))
                .hasSize(64)
                .matches("[0-9a-f]+");
    }
}
