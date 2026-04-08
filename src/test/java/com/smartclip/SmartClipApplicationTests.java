package com.smartclip;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "smartclip.data-dir=target/test-data",
        "smartclip.scheduler.tick-ms=60000",
        "spring.datasource.url=jdbc:sqlite:target/test-data/smartclip-test.db",
        "spring.task.scheduling.enabled=false"
})
/**
 * 应用上下文集成测试，验证 Spring Boot、Flyway 和 SQLite 能共同启动。
 */
class SmartClipApplicationTests {

    @Test
    /**
     * 加载完整应用上下文，确保基础 Bean 装配和数据库迁移可用。
     */
    void contextLoads() {
    }
}
