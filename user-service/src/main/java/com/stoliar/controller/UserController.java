package com.stoliar.controller;

import com.stoliar.dto.UserCreateDTO;
import com.stoliar.dto.UserDTO;
import com.stoliar.response.ApiResponse;
import com.stoliar.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Validated
@Slf4j
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {

    private final UserService userService;

    @Value("${api.gateway.internal-token}")
    private String apiGatewayInternalToken;

    private final int index = 7;

    @Operation(summary = "Create user", description = "Create a new user (ADMIN only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@Valid @RequestBody UserCreateDTO userCreateDTO) {
        log.info("Creating new user with email: {}", userCreateDTO.getEmail());
        UserDTO createdUser = userService.createUser(userCreateDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdUser, "User created successfully"));
    }

    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {

        log.info("Fetching user by id: {}", id);
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    @Operation(summary = "Get all users", description = "Retrieve paginated list of all users (ADMIN only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> getAllUsers(
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 10)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field (default: createdAt)") @RequestParam(defaultValue = "createdAt") String sort) {

        log.info("Fetching all users - page: {}, size: {}, sort: {}", page, size, sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<UserDTO> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));
    }

    @Operation(summary = "Get users with filters", description = "Retrieve filtered and paginated list of users (ADMIN only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Filtered users retrieved successfully")
    })
    @GetMapping("/filter")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> getUsersWithFilters(
            @Parameter(description = "First name filter") @RequestParam(required = false) String firstName,
            @Parameter(description = "Surname filter") @RequestParam(required = false) String surname,
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 10)") @RequestParam(defaultValue = "10") int size) {

        log.info("Filtering users - firstName: {}, surname: {}", firstName, surname);
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDTO> users = userService.getUsersWithFilters(firstName, surname, pageable);
        return ResponseEntity.ok(ApiResponse.success(users, "Filtered users retrieved successfully"));
    }

    @Operation(summary = "Update user", description = "Update an existing user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO) {

        log.info("Updating user with id: {}", id);
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "User updated successfully"));
    }

    @Operation(summary = "Update user status", description = "Activate or deactivate a user (ADMIN only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> updateUserStatus(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @Parameter(description = "Activation status", required = true) @RequestParam boolean active) {

        log.info("Updating user status - id: {}, active: {}", id, active);
        UserDTO updateUser = userService.updateUserStatus(id, active);
        String message = active ? "User activated successfully" : "User deactivated successfully";
        return ResponseEntity.ok(ApiResponse.success(updateUser, message));
    }

    @Operation(summary = "Delete user", description = "Delete a user (ADMIN only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {

        log.info("Deleting user with id: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete user for rollback", description = "Delete user by ID (for internal use by API Gateway)")
    @DeleteMapping("/internal/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUserForRollback(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @RequestHeader(value = "X-Service-Name", required = false) String serviceName,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Internal delete user for rollback, id: {}", id);

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
            log.warn("Invalid internal token received: {}", token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid internal token"));
        }

        try {
            userService.deleteUser(id);
            log.info("User deleted for rollback, id: {}", id);
            return ResponseEntity.ok(ApiResponse.success(null, "User deleted for rollback"));
        } catch (Exception e) {
            log.error("Error deleting user for rollback: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get user ID by email",
            description = "Retrieve user ID by email (ADMIN only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User ID retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")})
    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<Long>> getUserIdByEmail(
            @Parameter(description = "User email", required = true)
            @PathVariable @Email String email) {

        log.info("Getting user ID by email: {}", email);
        Long userId = userService.getUserIdByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(userId, "User ID retrieved successfully"));
    }
}