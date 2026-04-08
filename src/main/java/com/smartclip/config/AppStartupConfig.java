package com.smartclip.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
/**
 * 应用启动初始化配置，确保本地 data 目录在数据库连接和文件写入前可用。
 */
public class AppStartupConfig implements ApplicationRunner {

    private final Path dataDir;

    public AppStartupConfig(@Value("${smartclip.data-dir:data}") String dataDir) {
        this.dataDir = Path.of(dataDir);
    }

    @Override
    /**
     * Spring Boot 启动后检查并创建本地数据目录。
     */
    public void run(ApplicationArguments args) throws IOException {
        Files.createDirectories(dataDir);
        log.info("SmartClip data directory ready: {}", dataDir.toAbsolutePath());
    }
}
