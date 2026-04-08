package com.smartclip.clip;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartclip.clip.enums.ClipType;
import com.smartclip.clip.service.ClipTypeDetectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 文本类型识别服务单元测试，覆盖 MVP 要求中的主要类型判断。
 */
class ClipTypeDetectServiceTest {

    private ClipTypeDetectService service;

    @BeforeEach
    /**
     * 每个用例前创建独立服务实例，避免状态污染。
     */
    void setUp() {
        service = new ClipTypeDetectService(new ObjectMapper());
    }

    @Test
    /**
     * 验证 URL 文本识别。
     */
    void detectsUrl() {
        assertThat(service.detect("https://example.com/docs")).isEqualTo(ClipType.URL);
    }

    @Test
    /**
     * 验证 JSON 文本识别。
     */
    void detectsJson() {
        assertThat(service.detect("{\"name\":\"SmartClip\"}")).isEqualTo(ClipType.JSON);
    }

    @Test
    /**
     * 验证 SQL 文本识别。
     */
    void detectsSql() {
        assertThat(service.detect("select * from clip_item")).isEqualTo(ClipType.SQL);
    }

    @Test
    /**
     * 验证 Java 异常堆栈日志识别。
     */
    void detectsJavaExceptionLog() {
        String log = """
                java.lang.IllegalStateException: boom
                    at com.smartclip.App.run(App.java:12)
                """;
        assertThat(service.detect(log)).isEqualTo(ClipType.JAVA_EXCEPTION_LOG);
    }

    @Test
    /**
     * 验证无法匹配特征时回退为普通文本。
     */
    void fallsBackToText() {
        assertThat(service.detect("plain note")).isEqualTo(ClipType.TEXT);
    }
}
