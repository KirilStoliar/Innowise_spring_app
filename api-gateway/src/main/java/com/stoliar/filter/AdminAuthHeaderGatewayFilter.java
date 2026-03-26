package com.stoliar.filter;

import com.stoliar.admin.AdminTokenManager;
import com.stoliar.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthHeaderGatewayFilter implements GatewayFilter, Ordered {

    private final AdminTokenManager adminTokenManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final int index = 7;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() != null ?
                exchange.getRequest().getMethod().name() : "UNKNOWN";

        log.info("AdminAuthHeaderGatewayFilter: {} {}", method, path);

        if (path != null && path.equals("/api/v1/auth/register")) {
            log.info("Matched /api/v1/auth/register endpoint");

            // Проверяем, есть ли валидный Authorization header от клиента
            String clientAuthHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (clientAuthHeader != null && clientAuthHeader.startsWith("Bearer ")) {
                String clientToken = clientAuthHeader.substring(index);

                // Проверяем валидность клиентского токена
                if (jwtTokenProvider.validateToken(clientToken)) {
                    // Проверяем роль в токене
                    String role = jwtTokenProvider.getRoleFromToken(clientToken);

                    if ("ADMIN".equals(role)) {
                        log.info("Client provided valid ADMIN token, using it for /register");

                        // Клиент-админ предоставил валидный токен - используем его
                        ServerHttpRequest mutated = exchange.getRequest().mutate()
                                .header("X-Service-Name", "api-gateway")
                                .header("X-Auth-Source", "client-admin")
                                // НЕ меняем Authorization header!
                                .build();

                        return chain.filter(exchange.mutate().request(mutated).build());
                    } else {
                        log.warn("Client provided token with role '{}' - not ADMIN", role);
                        // Продолжаем использовать admin token из менеджера
                    }
                } else {
                    log.warn("Client provided invalid token, using admin manager token");
                }
            }

            // Если клиент не предоставил валидный ADMIN токен - используем токен из менеджера
            log.info("Using admin token from AdminTokenManager");

            String adminToken = adminTokenManager.getAdminToken();

            if (adminToken == null || adminToken.isBlank()) {
                log.error("AdminTokenManager returned null or empty token!");
                return sendErrorResponse(exchange, "Admin authentication unavailable");
            }

            // Проверяем валидность токена из менеджера
            if (!jwtTokenProvider.validateToken(adminToken)) {
                log.error("AdminTokenManager token is INVALID or EXPIRED!");
                log.info("Attempting to refresh admin token...");

                // Попытка синхронного обновления токена
                try {
                    adminToken = adminTokenManager.obtainAdminTokenReactive().block();

                    if (adminToken == null || !jwtTokenProvider.validateToken(adminToken)) {
                        return sendErrorResponse(exchange, "Failed to obtain valid admin token");
                    }

                    log.info("Admin token refreshed successfully");
                } catch (Exception e) {
                    log.error("Failed to refresh admin token: {}", e.getMessage());
                    return sendErrorResponse(exchange, "Admin service unavailable");
                }
            }

            log.debug("Adding Authorization header with admin manager token");

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("Authorization", "Bearer " + adminToken)
                    .header("X-Service-Name", "api-gateway")
                    .header("X-Auth-Source", "admin-manager")
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        }

        // Для всех других путей - пропускаем без изменений
        return chain.filter(exchange);
    }

    private Mono<Void> sendErrorResponse(ServerWebExchange exchange, String message) {
        log.error("AdminAuthHeaderGatewayFilter error: {}", message);
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String errorJson = String.format(
                "{\"success\":false,\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\"}",
                message,
                java.time.LocalDateTime.now()
        );

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(errorJson.getBytes()))
        );
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE + 15;
    }
}