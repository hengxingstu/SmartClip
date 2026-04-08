package com.smartclip.clip.service;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartclip.clip.enums.ClipType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
/**
 * 剪贴板文本类型识别服务，按 URL、JSON、异常日志、SQL、路径、命令、代码、普通文本的优先级判断。
 */
public class ClipTypeDetectService {

    private static final Pattern JAVA_EXCEPTION_PATTERN = Pattern.compile(
            "(?is).*(Exception|Error).*\\R\\s+at\\s+[\\w.$]+\\([^)]*\\).*");
    private static final Pattern SQL_PATTERN = Pattern.compile(
            "(?is)^\\s*(select|insert|update|delete|create|alter|drop|with)\\b.*");
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile(
            "(?is)^\\s*([a-z]:\\\\|\\\\\\\\[\\w.$-]+\\\\|/([\\w ._-]+/)+).+");
    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "(?is)^\\s*(git|npm|pnpm|yarn|mvn|gradle|java|docker|kubectl|dir|cd|copy|xcopy|curl|ssh)\\b.*");
    private static final Pattern CODE_PATTERN = Pattern.compile(
            "(?is).*(\\b(class|interface|enum|function|const|let|var|import|package|public\\s+static|return)\\b|[{};]).*");

    private final ObjectMapper objectMapper;

    /**
     * 根据内容特征返回基础文本类型；无法识别时返回 TEXT。
     */
    public ClipType detect(String content) {
        String text = content == null ? "" : content.strip();
        if (text.isEmpty()) {
            return ClipType.TEXT;
        }
        if (isUrl(text)) {
            return ClipType.URL;
        }
        if (isJson(text)) {
            return ClipType.JSON;
        }
        if (JAVA_EXCEPTION_PATTERN.matcher(text).matches()) {
            return ClipType.JAVA_EXCEPTION_LOG;
        }
        if (SQL_PATTERN.matcher(text).matches()) {
            return ClipType.SQL;
        }
        if (FILE_PATH_PATTERN.matcher(text).matches()) {
            return ClipType.FILE_PATH;
        }
        if (COMMAND_PATTERN.matcher(text).matches()) {
            return ClipType.COMMAND;
        }
        if (CODE_PATTERN.matcher(text).matches()) {
            return ClipType.CODE;
        }
        return ClipType.TEXT;
    }

    /**
     * 使用协议和 URI 基础校验判断 HTTP/HTTPS URL。
     */
    private boolean isUrl(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false;
        }
        try {
            URI uri = URI.create(text);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * 使用 Jackson 解析判断文本是否为合法 JSON 对象或数组。
     */
    private boolean isJson(String text) {
        if (!((text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]")))) {
            return false;
        }
        try {
            objectMapper.readTree(text);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
