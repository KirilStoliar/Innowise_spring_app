package com.stoliar.config;

import com.stoliar.filter.AdminAuthHeaderGatewayFilter;
import com.stoliar.filter.InternalTokenGatewayFilter;
import com.stoliar.filter.RequestLoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GatewayConfig {

    @Value("${gateway.auth.url:http://auth-service:8081}")
    private String authServiceUrl;

    @Value("${gateway.user.url:http://user-service:8080}")
    private String userServiceUrl;

    @Value("${gateway.order.url:http://order-service:8082}")
    private String orderServiceUrl;

    @Value("${gateway.payment.url:http://payment-service:8084}")
    private String paymentServiceUrl;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               InternalTokenGatewayFilter internalTokenFilter,
                               AdminAuthHeaderGatewayFilter adminAuthFilter,
                               RequestLoggingFilter requestLoggingFilter) {

        log.info("Configuring gateway routes...");

        return builder.routes()
                // AUTH — REGISTER (сначала admin фильтр!)
                .route("auth-register", r -> r.path("/api/v1/auth/register")
                        .filters(f -> f
                                .filter(adminAuthFilter)
                                .filter(requestLoggingFilter)
                        )
                        .uri(authServiceUrl))

                // AUTH — LOGIN и другие (без admin фильтра)
                .route("auth-login", r -> r.path("/api/v1/auth/login")
                        .filters(f -> f.filter(requestLoggingFilter))
                        .uri(authServiceUrl))

                .route("auth-validate", r -> r.path("/api/v1/auth/validate")
                        .filters(f -> f.filter(requestLoggingFilter))
                        .uri(authServiceUrl))

                .route("auth-refresh", r -> r.path("/api/v1/auth/refresh")
                        .filters(f -> f.filter(requestLoggingFilter))
                        .uri(authServiceUrl))

                .route("auth-rollback", r -> r.path("/api/v1/auth/rollback/**")
                        .filters(f -> f.filter(requestLoggingFilter))
                        .uri(authServiceUrl))

                // AUTH — INTERNAL
                .route("auth-internal", r -> r.path("/api/v1/auth/internal/**")
                        .filters(f -> f.filter(internalTokenFilter))
                        .uri(authServiceUrl))

                // USER
                .route("user-service", r -> r.path("/api/v1/users/**")
                        .filters(f -> f.filter(requestLoggingFilter))
                        .uri(userServiceUrl))

                // ORDER
                .route("order-service", r -> r.path("/api/v1/orders/**",
                                "/api/v1/items/**", "/api/v1/order-items/**")
                        .filters(f -> f.filter(requestLoggingFilter))
                        .uri(orderServiceUrl))

                // PAYMENT
                .route("payment-service", r -> r.path("/api/v1/payments/**")
                        .filters(f -> f.filter(requestLoggingFilter))
                        .uri(paymentServiceUrl))

                .build();
    }
}