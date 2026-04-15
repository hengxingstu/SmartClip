package com.smartclip.config;

import com.smartclip.clip.enums.ClipListView;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
/**
 * Web 层扩展配置预留类，当前 MVP 主要使用 Spring Boot 默认静态资源映射。
 */
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, ClipListView.class, source -> {
            if (source == null || source.isBlank()) {
                return null;
            }
            return ClipListView.fromRequestValue(source);
        });
    }
}
