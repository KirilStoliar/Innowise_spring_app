package com.stoliar.service;


import com.stoliar.dto.LoginRequest;
import com.stoliar.dto.TokenResponse;
import com.stoliar.dto.TokenValidationResponse;
import com.stoliar.dto.UserCredentialsRequest;
import com.stoliar.entity.UserCredentials;
import com.stoliar.response.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    UserCredentials saveUserCredentials(UserCredentialsRequest request, String adminToken);
    TokenResponse login(LoginRequest request);
    TokenResponse refreshToken(String refreshToken);
    TokenValidationResponse validateToken(String token);
    ResponseEntity<ApiResponse<Void>> deleteUserForRollback(Long id);
    ResponseEntity<ApiResponse<Void>> deleteUserAsAdmin(Long credentialsId, String adminToken);
}