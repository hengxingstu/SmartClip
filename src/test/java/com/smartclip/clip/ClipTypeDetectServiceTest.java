package com.smartclip.clip;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartclip.clip.enums.ClipType;
import com.smartclip.clip.service.ClipTypeDetectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 文本类型识别服务单元测试，覆盖 MVP 阶段需要支持的主要类型判断。
 */
class ClipTypeDetectServiceTest {

    private ClipTypeDetectService service;

    @BeforeEach
    /**
     * 每个用例执行前都创建新的服务实例，避免测试之间共享状态。
     */
    void setUp() {
        service = new ClipTypeDetectService(new ObjectMapper());
    }

    @Test
    /**
     * 这个用例验证标准 HTTP URL 会被识别为 URL 类型。
     */
    void detectsUrl() {
        assertThat(service.detect("https://example.com/docs")).isEqualTo(ClipType.URL);
    }

    @Test
    /**
     * 这个用例验证 JSON 对象字符串会被识别为 JSON 类型。
     */
    void detectsJson() {
        assertThat(service.detect("{\"name\":\"SmartClip\"}")).isEqualTo(ClipType.JSON);
    }

    @Test
    /**
     * 这个用例验证常见 SQL 查询语句会被识别为 SQL 类型。
     */
    void detectsSql() {
        assertThat(service.detect("select * from clip_item")).isEqualTo(ClipType.SQL);
    }

    @Test
    /**
     * 这个用例验证 Java 异常堆栈日志会被识别为异常日志类型。
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
     * 这个用例验证当文本不匹配任何特殊规则时会回退为普通文本类型。
     */
    void fallsBackToText() {
        assertThat(service.detect("plain note")).isEqualTo(ClipType.TEXT);
    }
}
