package com.stoliar.filter;

import com.stoliar.admin.AdminTokenManager;
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

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() != null ?
                exchange.getRequest().getMethod().name() : "UNKNOWN";

        log.info("AdminAuthHeaderGatewayFilter: {} {}", method, path);

        if (path != null && path.equals("/api/v1/auth/register")) {
            log.info("Matched /api/v1/auth/register endpoint");

            String token = adminTokenManager.getAdminToken();
            log.info("Admin token status: {}", token != null ? "AVAILABLE" : "NULL");
            log.info("Token length: {}", token != null ? token.length() : 0);

            if (token == null || token.isBlank()) {
                log.error("No admin token available!");
                exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                return exchange.getResponse().setComplete();
            }

            log.info("Adding Authorization header with token (first 50 chars): {}",
                    token.length() > 50 ? token.substring(0, 50) + "..." : token);

            // Логируем текущие заголовки
            log.info("Original headers:");
            exchange.getRequest().getHeaders().forEach((key, value) ->
                    log.info("  {}: {}", key, value));

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("Authorization", "Bearer " + token)
                    .header("X-Service-Name", "api-gateway")
                    .build();

            // Логируем измененные заголовки
            log.info("Modified headers:");
            mutated.getHeaders().forEach((key, value) ->
                    log.info("  {}: {}", key, value));

            return chain.filter(exchange.mutate().request(mutated).build());
        }

        log.info("Passing through without modification");
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE + 5; // Выполнится ДО JWT фильтра
    }
}