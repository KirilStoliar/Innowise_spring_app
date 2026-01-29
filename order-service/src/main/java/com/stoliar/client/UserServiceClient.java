package com.stoliar.client;

import com.stoliar.dto.user.UserApiResponse;
import com.stoliar.dto.user.UserInfoDto;
import com.stoliar.util.ServiceTokenProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final ServiceTokenProvider serviceTokenProvider;

    @Value("${user.service.url:http://localhost:8080}")
    private String userServiceUrl;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "userService")
    public UserInfoDto getUserById(Long userId) {

        log.info("Calling User Service for userId: {}", userId);

        String url = userServiceUrl + "/api/v1/users/" + userId;

        HttpHeaders headers = createServiceHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<UserApiResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                UserApiResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Invalid response from User Service");
        }

        UserApiResponse apiResponse = response.getBody();

        if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
            throw new IllegalStateException("User service returned unsuccessful response");
        }

        return apiResponse.getData();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailFallback")
    @Retry(name = "userService")
    public UserInfoDto getUserByEmail(String email) {
        log.info("Calling User Service for email: {}", email);

        String url = userServiceUrl + "/api/v1/users/email/" + email;

        HttpHeaders headers = createServiceHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<UserInfoDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                UserInfoDto.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        } else {
            log.error("Failed to get user info by email. Status: {}", response.getStatusCode());
            throw new RuntimeException("Failed to get user info from user service");
        }
    }

    private HttpHeaders createServiceHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String serviceToken = serviceTokenProvider.generateServiceToken();
        headers.setBearerAuth(serviceToken);
        headers.set("X-Service-Name", "order-service");
        return headers;
    }

    // Fallback методы
    public UserInfoDto getUserByIdFallback(Long userId, Exception e) {
        log.warn("Circuit Breaker Fallback triggered for userId: {}. Error: {}", userId, e.getMessage());
        return createFallbackUser(userId);
    }

    public UserInfoDto getUserByEmailFallback(String email, Exception e) {
        log.warn("Circuit Breaker Fallback triggered for email: {}. Error: {}", email, e.getMessage());
        return createFallbackUser(email);
    }

    private UserInfoDto createFallbackUser(Long ignoredUserId) {
        UserInfoDto fallback = new UserInfoDto();
        fallback.setId(-1L);
        fallback.setEmail("service@unavailable.com");
        fallback.setName("Service");
        fallback.setSurname("Temporarily Unavailable");
        fallback.setActive(false);
        return fallback;
    }

    private UserInfoDto createFallbackUser(String email) {
        UserInfoDto fallback = new UserInfoDto();
        fallback.setId(-1L);
        fallback.setEmail(email);
        fallback.setName("Service");
        fallback.setSurname("Temporarily Unavailable");
        fallback.setActive(false);
        return fallback;
    }
}