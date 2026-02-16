package com.stoliar.controller;

import com.stoliar.response.ApiResponse;
import com.stoliar.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/rollback")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Rollback", description = "API для отката создания пользователей")
public class ManualRollbackController {

    private final AuthService authService;
    private static final int index = 7;

    @Operation(summary = "Delete user for rollback",
            description = "Удаление пользователя при откате операции (только ADMIN)")
    @DeleteMapping("/{credentialsId}")
    public ResponseEntity<ApiResponse<Void>> deleteUserForRollback(
            @PathVariable Long credentialsId,
            HttpServletRequest request) {

        String token = getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authorization token is required"));
        }

        log.info("Manual rollback requested for credentials id: {}", credentialsId);

        authService.deleteUserAsAdmin(credentialsId, token);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Rollback completed successfully"));
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(index);
        }
        return null;
    }
}