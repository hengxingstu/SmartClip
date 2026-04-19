package com.smartclip.clip.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartclip.clip.enums.ClipType;
import com.smartclip.clip.model.ClipClassification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClipClassificationService {

    private static final int TITLE_MAX_LENGTH = 80;
    private static final Pattern SQL_TABLE_PATTERN = Pattern.compile(
            "(?is)\\b(from|into|update|table)\\s+([\\w.]+)");
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile(
            "(?m)([\\w.$]+(?:Exception|Error))(?::\\s*([^\\r\\n]+))?");
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\bclass\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("\\bfunction\\s+([A-Za-z_$][\\w$]*)");

    private final ClipTypeDetectService clipTypeDetectService;
    private final ClipPreviewService clipPreviewService;
    private final ObjectMapper objectMapper;

    public ClipClassification classify(String content) {
        String text = content == null ? "" : content.strip();
        ClipType type = clipTypeDetectService.detect(text);
        return switch (type) {
            case URL -> classifyUrl(text);
            case JSON -> classifyJson(text);
            case SQL -> classifySql(text);
            case COMMAND -> classifyCommand(text);
            case JAVA_EXCEPTION_LOG -> classifyJavaException(text);
            case FILE_PATH -> classifyFilePath(text);
            case CODE -> classifyCode(text);
            case TEXT -> basic(type, "NOTE", clipPreviewService.buildTitle(text), "text");
        };
    }

    private ClipClassification classifyUrl(String text) {
        Set<String> tags = tags("url");
        String subType = "GENERAL";
        String title = clipPreviewService.buildTitle(text);
        try {
            URI uri = URI.create(text);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath();
            String lower = text.toLowerCase(Locale.ROOT);
            title = abbreviate(host + path, TITLE_MAX_LENGTH);
            if (host.contains("github.com")) {
                subType = "GITHUB";
                tags.add("github");
            } else if (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1")) {
                subType = "LOCALHOST";
                tags.add("local");
            } else if (host.startsWith("api.") || lower.contains("/api")) {
                subType = "API";
                tags.add("api");
            } else if (lower.contains("docs") || lower.contains("/doc")) {
                subType = "DOCS";
                tags.add("docs");
            }
        } catch (RuntimeException ignored) {
            // URL type has already been detected. Keep the safe fallback title.
        }
        return new ClipClassification(ClipType.URL, subType, title, List.copyOf(tags));
    }

    private ClipClassification classifyJson(String text) {
        String subType = "OBJECT";
        String title = "JSON object";
        try {
            JsonNode node = objectMapper.readTree(text);
            if (node.isArray()) {
                subType = "ARRAY";
                title = "JSON array";
            } else if (node.isObject()) {
                List<String> keys = new ArrayList<>();
                Iterator<String> fieldNames = node.fieldNames();
                while (fieldNames.hasNext() && keys.size() < 3) {
                    keys.add(fieldNames.next());
                }
                if (!keys.isEmpty()) {
                    title = "JSON object: " + String.join(", ", keys);
                }
            }
        } catch (Exception ignored) {
            title = clipPreviewService.buildTitle(text);
        }
        return basic(ClipType.JSON, subType, title, "json");
    }

    private ClipClassification classifySql(String text) {
        String action = firstToken(text).toUpperCase(Locale.ROOT);
        String subType = switch (action) {
            case "SELECT" -> "SELECT";
            case "INSERT" -> "INSERT";
            case "UPDATE" -> "UPDATE";
            case "DELETE" -> "DELETE";
            case "WITH" -> "WITH";
            case "CREATE", "ALTER", "DROP" -> "DDL";
            default -> "SQL";
        };
        Set<String> tags = tags("sql");
        if ("SELECT".equals(subType) || "WITH".equals(subType)) {
            tags.add("query");
        } else if ("DDL".equals(subType)) {
            tags.add("ddl");
        } else {
            tags.add("mutation");
        }

        String table = findSqlTable(text);
        String title = table == null ? clipPreviewService.buildTitle(text) : "SQL " + subType + " " + table;
        return new ClipClassification(ClipType.SQL, subType, title, List.copyOf(tags));
    }

    private ClipClassification classifyCommand(String text) {
        String command = firstToken(text).toLowerCase(Locale.ROOT);
        String subType = switch (command) {
            case "git" -> "GIT";
            case "npm", "pnpm", "yarn" -> "NPM";
            case "mvn" -> "MAVEN";
            case "docker" -> "DOCKER";
            case "kubectl" -> "KUBECTL";
            case "ssh" -> "SSH";
            case "curl" -> "CURL";
            case "java" -> "JAVA";
            default -> "OTHER";
        };
        Set<String> tags = tags("command");
        if (!command.isBlank()) {
            tags.add(command);
        }
        return new ClipClassification(ClipType.COMMAND, subType,
                abbreviate(command.toUpperCase(Locale.ROOT) + " " + text.substring(command.length()).strip(), TITLE_MAX_LENGTH),
                List.copyOf(tags));
    }

    private ClipClassification classifyJavaException(String text) {
        Matcher matcher = EXCEPTION_PATTERN.matcher(text);
        String subType = "EXCEPTION";
        String title = clipPreviewService.buildTitle(text);
        if (matcher.find()) {
            String exceptionName = matcher.group(1);
            subType = exceptionName.substring(exceptionName.lastIndexOf('.') + 1);
            String message = matcher.group(2);
            title = message == null || message.isBlank() ? subType : subType + ": " + message.strip();
        }
        return basic(ClipType.JAVA_EXCEPTION_LOG, subType, abbreviate(title, TITLE_MAX_LENGTH), "java", "exception");
    }

    private ClipClassification classifyFilePath(String text) {
        String subType;
        if (text.startsWith("\\\\")) {
            subType = "UNC_PATH";
        } else if (text.matches("(?is)^[a-z]:\\\\.*")) {
            subType = "WINDOWS_PATH";
        } else {
            subType = "UNIX_PATH";
        }
        return basic(ClipType.FILE_PATH, subType, lastPathSegment(text), "path");
    }

    private ClipClassification classifyCode(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        String subType = "UNKNOWN";
        Set<String> tags = tags("code");
        if (lower.contains("public class") || lower.startsWith("package ") || lower.contains("import java.")) {
            subType = "JAVA";
            tags.add("java");
        } else if (lower.contains("function ") || lower.contains("const ") || lower.contains("let ")
                || lower.contains("import ") || lower.contains("=>")) {
            subType = "JAVASCRIPT";
            tags.add("javascript");
        }

        String title = findCodeTitle(text, subType);
        return new ClipClassification(ClipType.CODE, subType, title, List.copyOf(tags));
    }

    private ClipClassification basic(ClipType type, String subType, String title, String... tagNames) {
        return new ClipClassification(type, subType, abbreviate(title, TITLE_MAX_LENGTH), List.copyOf(tags(tagNames)));
    }

    private Set<String> tags(String... names) {
        Set<String> tags = new LinkedHashSet<>();
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                tags.add(name.toLowerCase(Locale.ROOT));
            }
        }
        return tags;
    }

    private String firstToken(String text) {
        String trimmed = text == null ? "" : text.strip();
        int spaceIndex = trimmed.indexOf(' ');
        int lineIndex = trimmed.indexOf('\n');
        int end = trimmed.length();
        if (spaceIndex >= 0) {
            end = Math.min(end, spaceIndex);
        }
        if (lineIndex >= 0) {
            end = Math.min(end, lineIndex);
        }
        return end <= 0 ? "" : trimmed.substring(0, end);
    }

    private String findSqlTable(String text) {
        Matcher matcher = SQL_TABLE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(2) : null;
    }

    private String lastPathSegment(String text) {
        String normalized = text.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        String segment = index >= 0 ? normalized.substring(index + 1) : normalized;
        return segment.isBlank() ? clipPreviewService.buildTitle(text) : abbreviate(segment, TITLE_MAX_LENGTH);
    }

    private String findCodeTitle(String text, String subType) {
        Matcher classMatcher = CLASS_PATTERN.matcher(text);
        if (classMatcher.find()) {
            return ("JAVA".equals(subType) ? "Java class " : "Class ") + classMatcher.group(1);
        }
        Matcher functionMatcher = FUNCTION_PATTERN.matcher(text);
        if (functionMatcher.find()) {
            return "Function " + functionMatcher.group(1);
        }
        return clipPreviewService.buildTitle(text);
    }

    private String abbreviate(String text, int maxLength) {
        String value = text == null ? "" : text.strip();
        if (value.isBlank()) {
            return "Untitled clip";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
