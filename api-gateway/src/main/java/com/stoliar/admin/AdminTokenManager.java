package com.stoliar.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class AdminTokenManager {

    private final WebClient webClient;
    private final String authLoginUrl;
    private final String adminEmail;
    private final String adminPassword;
    private final AtomicReference<String> adminToken = new AtomicReference<>();

    public AdminTokenManager(WebClient webClient,
                             @Value("${gateway.auth.url:http://auth-service:8081}") String authServiceUrl,
                             @Value("${gateway-admin.email}") String adminEmail,
                             @Value("${gateway-admin.password}") String adminPassword) {
        this.webClient = webClient;
        this.authLoginUrl = authServiceUrl + "/api/v1/auth/login";
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void obtainAdminTokenOnStartup() {
        log.info("Obtaining admin token from {}", authLoginUrl);

        obtainAdminTokenReactive()
                .doOnSuccess(token -> {
                    adminToken.set(token);
                    log.info("Admin token obtained successfully (length={})", token.length());
                })
                .doOnError((Throwable error) -> {
                    log.error("Failed to obtain admin token: {}", error.getMessage());
                    log.warn("⚠Admin functionality (user registration) will be unavailable");
                    scheduleRetry(30); // Повтор через 30 секунд
                })
                .subscribe();
    }

    public Mono<String> obtainAdminTokenReactive() {
        return webClient.post()
                .uri(authLoginUrl)
                .bodyValue(Map.of("email", adminEmail, "password", adminPassword))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(10))
                        .doBeforeRetry(retrySignal ->
                                log.warn("Retrying admin token request, attempt {}/5",
                                        retrySignal.totalRetries() + 1)))
                .map(this::extractTokenFromResponse)
                .doOnError((Throwable throwable) ->
                        log.error("Failed after retries: {}", throwable.getMessage()));
    }

    private String extractTokenFromResponse(TokenResponse response) {
        if (response == null) {
            throw new RuntimeException("Empty response from auth service");
        }

        String token = null;

        // Проверяем разные возможные структуры ответа
        if (response.getData() != null && response.getData().getAccessToken() != null) {
            token = response.getData().getAccessToken();
        } else if (response.getAccessToken() != null) {
            token = response.getAccessToken();
        }

        if (token == null) {
            throw new RuntimeException("Token not found in response");
        }

        return token;
    }

    @Data
    private static class TokenResponse {
        private TokenData data;
        private String accessToken;

        @Data
        private static class TokenData {
            @JsonProperty("accessToken")
            private String accessToken;
        }
    }

    private void scheduleRetry(int delaySeconds) {
        Mono.delay(Duration.ofSeconds(delaySeconds))
                .doOnNext(v -> {
                    log.info("Retrying admin token acquisition after {} seconds...", delaySeconds);
                    obtainAdminTokenReactive()
                            .doOnSuccess(token -> {
                                adminToken.set(token);
                                log.info("Admin token obtained on retry (length={})", token.length());
                            })
                            .doOnError((Throwable error) ->
                                    log.error("Failed again: {}", error.getMessage()))
                            .subscribe();
                })
                .subscribe();
    }

    public String getAdminToken() {
        return adminToken.get();
    }

    public boolean isTokenAvailable() {
        return adminToken.get() != null;
    }

    public Mono<String> getAdminTokenReactive() {
        return Mono.fromCallable(() -> {
            String token = adminToken.get();
            if (token == null) {
                throw new RuntimeException("Admin token not available. " +
                        "Try checking if auth-service is running and admin credentials are correct.");
            }
            return token;
        });
    }
}