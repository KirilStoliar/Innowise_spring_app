package com.stoliar.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RequestLoggingFilter implements GatewayFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        log.info("INCOMING REQUEST: {} {}", request.getMethod(), request.getURI());
        log.info("ALL HEADERS:");
        request.getHeaders().forEach((key, value) ->
            log.info("    {}: {}", key, value));

        return chain.filter(exchange)
            .doOnSuccess(v ->
                log.info("COMPLETED: {} {}", request.getMethod(), request.getURI()))
            .doOnError(e ->
                log.error("ERROR: {} {} - {}", request.getMethod(),
                    request.getURI(), e.getMessage()));
    }
}