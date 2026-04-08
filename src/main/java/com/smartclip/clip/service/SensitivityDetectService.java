package com.smartclip.clip.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
/**
 * 敏感内容识别服务，MVP 阶段使用轻量正则识别口令、密钥、token 等文本。
 */
public class SensitivityDetectService {

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?is).*(password\\s*[=:]|passwd\\s*[=:]|secret\\s*[=:]|token\\s*[=:]|api[_-]?key\\s*[=:]|-----BEGIN .*PRIVATE KEY-----).*");

    /**
     * 判断文本是否命中敏感内容规则。
     */
    public boolean isSensitive(String content) {
        return content != null && SENSITIVE_PATTERN.matcher(content).matches();
    }
}
