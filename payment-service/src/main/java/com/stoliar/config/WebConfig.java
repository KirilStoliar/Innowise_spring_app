package com.stoliar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, LocalDateTime.class, source -> {
            // Убираем возможные проблемы с URL encoding
            String cleanSource = source.replace("%3A", ":");
            
            // Пробуем разные форматы
            try {
                return LocalDateTime.parse(cleanSource, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e1) {
                try {
                    // Если нет секунд
                    return LocalDateTime.parse(cleanSource + ":00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e2) {
                    try {
                        // Формат без секунд явно
                        return LocalDateTime.parse(cleanSource, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                    } catch (Exception e3) {
                        throw new IllegalArgumentException(
                            "Cannot parse date: " + source + 
                            ". Supported formats: yyyy-MM-dd'T'HH:mm:ss, yyyy-MM-dd'T'HH:mm");
                    }
                }
            }
        });
    }
}