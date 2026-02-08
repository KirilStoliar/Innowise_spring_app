package com.stoliar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoliar.dto.PaymentRequest;
import com.stoliar.dto.PaymentResponse;
import com.stoliar.entity.enums.PaymentStatus;
import com.stoliar.service.PaymentService;
import com.stoliar.util.JwtTokenProvider;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "10", roles = {"USER"})
    void createPayment_success() throws Exception {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(new ObjectId().toString())
                .orderId(1L)
                .userId(10L)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        when(paymentService.createPayment(
                any(PaymentRequest.class),
                any(),  // Authentication
                eq(null)
        )).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "10", roles = {"USER"})
    void getPaymentById_success() throws Exception {
        // Given
        String paymentId = new ObjectId().toString();

        PaymentResponse response = PaymentResponse.builder()
                .id(paymentId)
                .orderId(1L)
                .userId(10L)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("50.00"))
                .build();
        
        when(paymentService.getPaymentById(
                eq(paymentId),
                any()  // Authentication
        )).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(paymentId))
                .andExpect(jsonPath("$.data.userId").value(10L));
    }

    @Test
    @WithMockUser(username = "10", roles = {"USER"})
    void getPaymentsByUserId_success() throws Exception {
        // Given
        Long userId = 10L;

        PaymentResponse response1 = PaymentResponse.builder()
                .id(new ObjectId().toString())
                .orderId(1L)
                .userId(userId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("50.00"))
                .build();

        PaymentResponse response2 = PaymentResponse.builder()
                .id(new ObjectId().toString())
                .orderId(2L)
                .userId(userId)
                .status(PaymentStatus.FAILED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        when(paymentService.getPaymentsByUserId(
                eq(userId),
                any()  // Authentication
        )).thenReturn(java.util.List.of(response1, response2));

        // When & Then
        mockMvc.perform(get("/api/v1/payments/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].userId").value(userId))
                .andExpect(jsonPath("$.data[1].userId").value(userId));
    }

    @Test
    @WithMockUser(username = "10", roles = {"USER"})
    void getPaymentsByOrderId_success() throws Exception {
        // Given
        Long orderId = 1L;
        Long userId = 10L;

        PaymentResponse response = PaymentResponse.builder()
                .id(new ObjectId().toString())
                .orderId(orderId)
                .userId(userId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("50.00"))
                .build();

        when(paymentService.getPaymentsByOrderId(
                eq(orderId),
                any()  // Authentication
        )).thenReturn(java.util.List.of(response));

        // When & Then
        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].orderId").value(orderId))
                .andExpect(jsonPath("$.data[0].userId").value(userId));
    }

    @Test
    @WithMockUser(username = "1", roles = {"ADMIN"})
    void getTotalSum_success_asAdmin() throws Exception {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 28, 10, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 28, 18, 0, 0);

        when(paymentService.getTotalSumByDateRange(
                eq(startDate),
                eq(endDate),
                any()  // Authentication
        )).thenReturn(new BigDecimal("500.00"));

        // When & Then
        mockMvc.perform(get("/api/v1/payments/total")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(500.00));
    }

    @Test
    @WithMockUser(username = "10", roles = {"USER"})
    void getTotalSumByUserId_success() throws Exception {
        // Given
        Long userId = 10L;
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 28, 10, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 28, 18, 0, 0);

        when(paymentService.getTotalSumByUserIdAndDateRange(
                eq(userId),
                eq(startDate),
                eq(endDate),
                any()  // Authentication
        )).thenReturn(new BigDecimal("150.00"));

        // When & Then
        mockMvc.perform(get("/api/v1/payments/user/{userId}/total", userId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(150.00));
    }

    @Test
    @WithMockUser(username = "10", roles = {"USER"})
    void createPayment_invalidRequest_returnsBadRequest() throws Exception {
        // Given - невалидный запрос без orderId
        PaymentRequest request = PaymentRequest.builder()
                .orderId(null) // null - невалидно
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "10", roles = {"USER"})
    void getPaymentsByCriteria_success() throws Exception {
        // Given
        Long userId = 10L;
        Long orderId = 1L;

        PaymentResponse response = PaymentResponse.builder()
                .id(new ObjectId().toString())
                .orderId(orderId)
                .userId(userId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("50.00"))
                .build();

        when(paymentService.getPaymentsByCriteria(
                eq(userId),
                eq(orderId),
                eq(PaymentStatus.COMPLETED),
                any()  // Authentication
        )).thenReturn(java.util.List.of(response));

        // When & Then
        mockMvc.perform(get("/api/v1/payments/search")
                        .param("userId", userId.toString())
                        .param("orderId", orderId.toString())
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(userId))
                .andExpect(jsonPath("$.data[0].orderId").value(orderId))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "10", roles = {"USER"})
    void getPaymentsByStatus_success() throws Exception {
        // Given
        Long userId = 10L;

        PaymentResponse response = PaymentResponse.builder()
                .id(new ObjectId().toString())
                .orderId(1L)
                .userId(userId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("50.00"))
                .build();

        when(paymentService.getPaymentsByStatus(
                eq(PaymentStatus.COMPLETED),
                any()  // Authentication
        )).thenReturn(java.util.List.of(response));

        // When & Then
        mockMvc.perform(get("/api/v1/payments/status/{status}", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data[0].userId").value(userId));
    }
}