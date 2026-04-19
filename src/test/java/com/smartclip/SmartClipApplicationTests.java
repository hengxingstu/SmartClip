package com.smartclip;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "smartclip.data-dir=target/test-data",
        "smartclip.scheduler.tick-ms=60000",
        "spring.datasource.url=jdbc:sqlite:target/test-data/smartclip-test.db",
        "spring.task.scheduling.enabled=false"
})
/**
 * 应用上下文集成测试，验证 Spring Boot、Flyway 和 SQLite 可以一起正常启动。
 */
class SmartClipApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    /**
     * 这个用例只负责加载完整应用上下文，确保基础 Bean 装配和数据库迁移没有在启动阶段失败。
     */
    void contextLoads() {
    }

    @Test
    /**
     * 这个用例验证 Phase 3 引入的标签表和索引已经通过 Flyway 正确建到测试数据库中。
     */
    void migratesPhase3TagTablesAndIndexes() {
        assertThat(countSqliteObjects("table", "tag")).isEqualTo(1);
        assertThat(countSqliteObjects("table", "clip_item_tag")).isEqualTo(1);
        assertThat(countSqliteObjects("index", "uk_tag_normalized_name")).isEqualTo(1);
        assertThat(countSqliteObjects("index", "idx_clip_item_type_sub_type")).isEqualTo(1);
    }

    /**
     * 这个辅助方法用于查询 sqlite_master，统计指定名称的表或索引是否存在。
     */
    private Integer countSqliteObjects(String type, String name) {
        return jdbcTemplate.queryForObject(
                "select count(*) from sqlite_master where type = ? and name = ?",
                Integer.class,
                type,
                name
        );
    }
}
