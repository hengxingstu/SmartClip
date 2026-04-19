package com.smartclip.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 哈希工具单元测试，确保去重依赖的哈希行为稳定且可预期。
 */
class HashUtilsTest {

    @Test
    /**
     * 这个用例验证不同平台的换行符在计算哈希前会先被统一归一化。
     */
    void normalizesLineEndingsBeforeHashing() {
        assertThat(HashUtils.sha256NormalizedText("a\r\nb\r"))
                .isEqualTo(HashUtils.sha256NormalizedText("a\nb\n"));
    }

    @Test
    /**
     * 这个用例验证 SHA-256 输出长度固定，并且结果是稳定的十六进制字符串。
     */
    void returnsStableSha256Hex() {
        assertThat(HashUtils.sha256("SmartClip"))
                .hasSize(64)
                .matches("[0-9a-f]+");
    }
}
