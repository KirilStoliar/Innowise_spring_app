package com.stoliar.filter;

import com.stoliar.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;
    private final int index = 7;

    // Пути, которые не требуют авторизации через JWT
    private final List<String> excludedPaths = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/internal/**",
            "/actuator/**");

    private final PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() != null ?
                exchange.getRequest().getMethod().name() : "UNKNOWN";

        log.debug("JwtAuthenticationGlobalFilter: {} {}", method, path);

        // Проверяем исключения (публичные эндпоинты)
        for (String pattern : excludedPaths) {
            if (pathMatcher.match(pattern, path)) {
                log.debug("Skipping JWT validation for excluded path: {}", path);
                return chain.filter(exchange);
            }
        }

        // Проверяем Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(index);
        }

        // Специальная обработка для /api/v1/auth/register
        if ("/api/v1/auth/register".equals(path)) {
            if (token == null) {
                log.warn("No token for register endpoint");
                return unauthorizedResponse(exchange, "Authorization required");
            }

            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Invalid token for register endpoint");
                return unauthorizedResponse(exchange, "Invalid token");
            }

            // Проверяем роль - должен быть ADMIN
            String role = jwtTokenProvider.getRoleFromToken(token);
            if (role == null || !"ADMIN".equals(role)) {
                log.warn("User with role {} attempted to access admin-only endpoint", role);
                return forbiddenResponse(exchange, "Admin access required");
            }

            // Если пользователь ADMIN - пропускаем
            log.info("Admin user (role: {}) accessing register endpoint", role);
            return chain.filter(exchange);
        }

        // Стандартная проверка для других защищенных эндпоинтов
        if (token == null) {
            log.warn("No token provided for protected endpoint: {}", path);
            return unauthorizedResponse(exchange, "Authorization required");
        }

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Invalid token for endpoint: {}", path);
            return unauthorizedResponse(exchange, "Invalid or expired token");
        }

        log.debug("JWT validation passed for endpoint: {}", path);
        return chain.filter(exchange);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        log.warn("Unauthorized: {} - {}", exchange.getRequest().getURI(), message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    private Mono<Void> forbiddenResponse(ServerWebExchange exchange, String message) {
        log.warn("Forbidden: {} - {}", exchange.getRequest().getURI(), message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        // Должен выполняться до AdminAuthHeaderGatewayFilter
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}