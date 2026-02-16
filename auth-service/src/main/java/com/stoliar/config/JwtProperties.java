package com.stoliar.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {
    private String secret;
    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration; // 24 часа
    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration; // 7 дней
}