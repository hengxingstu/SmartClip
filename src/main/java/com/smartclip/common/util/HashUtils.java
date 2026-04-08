package com.smartclip.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 哈希工具类，提供剪贴板文本去重所需的 SHA-256 和换行归一化能力。
 */
public final class HashUtils {

    private HashUtils() {
    }

    /**
     * 先归一化换行符，再计算 SHA-256，减少 Windows/Unix 换行差异导致的重复记录。
     */
    public static String sha256NormalizedText(String text) {
        return sha256(normalizeLineEndings(text));
    }

    /**
     * 将 CRLF/CR 统一为 LF。
     */
    public static String normalizeLineEndings(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }

    /**
     * 计算字符串的 SHA-256 十六进制摘要。
     */
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
