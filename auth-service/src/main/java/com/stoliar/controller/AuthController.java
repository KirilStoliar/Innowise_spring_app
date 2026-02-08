package com.stoliar.controller;

import com.stoliar.dto.*;
import com.stoliar.entity.UserCredentials;
import com.stoliar.response.ApiResponse;
import com.stoliar.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API для аутентификации и авторизации")
public class AuthController {

    private final AuthService authService;
    private final int index = 7;

    @Value("${api-gateway.internal-token}")
    private String apiGatewayInternalToken;

    @Operation(summary = "Save user credentials", description = "Save user credentials (ADMIN only)")
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserCredentials>> saveUserCredentials(
            @Valid @RequestBody UserCredentialsRequest request,
            HttpServletRequest httpRequest) {

        log.info("Saving user credentials for username: {}", request.getEmail());

        // Извлекаем токен из заголовка Authorization
        String token = getTokenFromRequest(httpRequest);
        if (!StringUtils.hasText(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authorization token is required"));
        }

        UserCredentials credentials = authService.saveUserCredentials(request, token);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(credentials, "User credentials saved successfully"));
    }

    @Operation(summary = "Create token", description = "Create JWT token for authentication")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> createToken(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login request for username: {}", request.getEmail());

        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse, "Login successful"));
    }

    @Operation(summary = "Validate token", description = "Validate JWT token")
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validateToken(
            @RequestHeader("Authorization") String authHeader) {

        log.info("Token validation request");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid Authorization header format. Expected: Bearer <token>"));
        }

        String token = authHeader.substring(index);
        TokenValidationResponse validationResponse = authService.validateToken(token);
        String message = validationResponse.isValid() ? "Token is valid" : "Token is invalid";

        return ResponseEntity.ok(ApiResponse.success(validationResponse, message));
    }

    @Operation(summary = "Refresh token", description = "Refresh JWT tokens using refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.info("Refresh token request");

        TokenResponse tokenResponse = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(tokenResponse, "Token refreshed successfully"));
    }

    @Operation(summary = "Delete user for rollback (only test, it use inside service)", description = "Delete user by ID (for internal use by API Gateway)")
    @DeleteMapping("/internal/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUserForRollback(
            @PathVariable Long id,
            @RequestHeader(value = "X-Service-Name", required = false) String serviceName,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Deleting user for rollback, id: {}", id);

        // Проверяем, что запрос от API Gateway
        if (!"api-gateway".equals(serviceName)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied. Only API Gateway can call this endpoint"));
        }

        // Проверяем внутренний токен
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authorization token required"));
        }

        String token = authHeader.substring(index);

        // Проверяем внутренний токен API Gateway
        if (!apiGatewayInternalToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid internal token"));
        }

        try {
            // Используем сервис для удаления пользователя
            authService.deleteUserForRollback(id);
            log.info("User deleted for rollback, id: {}", id);
            return ResponseEntity.ok(ApiResponse.success(null, "User deleted for rollback"));
        } catch (Exception e) {
            log.error("Error deleting user for rollback: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(index);
        }
        return null;
    }
}