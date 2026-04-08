package com.smartclip.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * 数据源初始化配置，创建 SQLite 数据库所在目录并交由 Spring Boot 构建 DataSource。
 */
public class DataSourceConfig {

    @Bean
    /**
     * 构建 SQLite DataSource；这里提前创建 data 目录，避免 Flyway 迁移时路径不存在。
     */
    public DataSource dataSource(
            DataSourceProperties properties,
            @Value("${smartclip.data-dir:data}") String dataDir) throws IOException {
        Files.createDirectories(Path.of(dataDir));
        return properties.initializeDataSourceBuilder().build();
    }
}
