package com.smartclip;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.smartclip.**.mapper")
@SpringBootApplication
/**
 * SmartClip 后端应用入口，负责启动 Spring Boot、定时任务和 MyBatis Mapper 扫描。
 */
public class SmartClipApplication {

    /**
     * 启动本地 SmartClip 服务。
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(SmartClipApplication.class);
        application.setHeadless(false);
        application.run(args);
    }
}
