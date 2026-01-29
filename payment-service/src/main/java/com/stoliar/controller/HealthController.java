package com.stoliar.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/actuator/health")
@RequiredArgsConstructor
public class HealthController implements HealthIndicator {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @GetMapping
    @Override
    public Health health() {
        try {
            // Проверяем соединение с Kafka
            kafkaTemplate.send("health-check", "ping").get(5, TimeUnit.SECONDS);
            log.debug("Kafka health check passed");

            return Health.up()
                    .withDetail("kafka", "connected")
                    .build();
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return Health.up() // Сервис работает, но Kafka недоступен
                    .withDetail("kafka", "disconnected")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}