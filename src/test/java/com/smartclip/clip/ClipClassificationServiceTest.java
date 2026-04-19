package com.smartclip.clip;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartclip.clip.enums.ClipType;
import com.smartclip.clip.model.ClipClassification;
import com.smartclip.clip.service.ClipClassificationService;
import com.smartclip.clip.service.ClipPreviewService;
import com.smartclip.clip.service.ClipTypeDetectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 分类服务单元测试，覆盖不同类型文本生成 subtype、标题和标签的规则。
 */
class ClipClassificationServiceTest {

    private ClipClassificationService service;

    @BeforeEach
    /**
     * 每个用例执行前都重新创建分类服务，保证分类规则测试之间互不影响。
     */
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new ClipClassificationService(
                new ClipTypeDetectService(objectMapper),
                new ClipPreviewService(),
                objectMapper
        );
    }

    @Test
    /**
     * 这个用例验证 GitHub URL 会被识别为 URL 类型，并补充 github 相关的 subtype 和标签。
     */
    void classifiesGithubUrl() {
        ClipClassification classification = service.classify("https://github.com/HengxingStu/SmartClip");

        assertThat(classification.type()).isEqualTo(ClipType.URL);
        assertThat(classification.subType()).isEqualTo("GITHUB");
        assertThat(classification.title()).contains("github.com");
        assertThat(classification.tagNames()).contains("url", "github");
    }

    @Test
    /**
     * 这个用例验证 JSON 数组会被分类为 ARRAY 子类型，并带上 json 标签。
     */
    void classifiesJsonArray() {
        ClipClassification classification = service.classify("[{\"name\":\"SmartClip\"}]");

        assertThat(classification.type()).isEqualTo(ClipType.JSON);
        assertThat(classification.subType()).isEqualTo("ARRAY");
        assertThat(classification.tagNames()).contains("json");
    }

    @Test
    /**
     * 这个用例验证 SELECT 语句会提取出表名，并生成便于展示的 SQL 标题。
     */
    void classifiesSqlSelect() {
        ClipClassification classification = service.classify("select * from clip_item where id = 1");

        assertThat(classification.type()).isEqualTo(ClipType.SQL);
        assertThat(classification.subType()).isEqualTo("SELECT");
        assertThat(classification.title()).isEqualTo("SQL SELECT clip_item");
        assertThat(classification.tagNames()).contains("sql", "query");
    }

    @Test
    /**
     * 这个用例验证常见命令行文本会被识别为命令类型，并保留对应命令标签。
     */
    void classifiesCommand() {
        ClipClassification classification = service.classify("git status --short");

        assertThat(classification.type()).isEqualTo(ClipType.COMMAND);
        assertThat(classification.subType()).isEqualTo("GIT");
        assertThat(classification.tagNames()).contains("command", "git");
    }

    @Test
    /**
     * 这个用例验证 Java 异常日志会提取异常名称和异常消息作为展示标题。
     */
    void classifiesJavaException() {
        ClipClassification classification = service.classify("""
                java.lang.IllegalStateException: boom
                    at com.smartclip.App.run(App.java:12)
                """);

        assertThat(classification.type()).isEqualTo(ClipType.JAVA_EXCEPTION_LOG);
        assertThat(classification.subType()).isEqualTo("IllegalStateException");
        assertThat(classification.title()).contains("boom");
        assertThat(classification.tagNames()).contains("java", "exception");
    }

    @Test
    /**
     * 这个用例验证 Windows 文件路径会被识别，并把最后一个路径片段作为标题。
     */
    void classifiesWindowsPath() {
        ClipClassification classification = service.classify("C:\\Users\\heng\\smartclip.txt");

        assertThat(classification.type()).isEqualTo(ClipType.FILE_PATH);
        assertThat(classification.subType()).isEqualTo("WINDOWS_PATH");
        assertThat(classification.title()).isEqualTo("smartclip.txt");
        assertThat(classification.tagNames()).contains("path");
    }

    @Test
    /**
     * 这个用例验证 Java 代码片段会被识别为 CODE，并尝试从类声明中提取标题。
     */
    void classifiesJavaCode() {
        ClipClassification classification = service.classify("public class SmartClip { }");

        assertThat(classification.type()).isEqualTo(ClipType.CODE);
        assertThat(classification.subType()).isEqualTo("JAVA");
        assertThat(classification.title()).isEqualTo("Java class SmartClip");
        assertThat(classification.tagNames()).contains("code", "java");
    }

    @Test
    /**
     * 这个用例验证普通文本在没有更具体规则命中时会回退为 NOTE 类型的文本分类。
     */
    void classifiesText() {
        ClipClassification classification = service.classify("plain note");

        assertThat(classification.type()).isEqualTo(ClipType.TEXT);
        assertThat(classification.subType()).isEqualTo("NOTE");
        assertThat(classification.tagNames()).containsExactly("text");
    }
}
