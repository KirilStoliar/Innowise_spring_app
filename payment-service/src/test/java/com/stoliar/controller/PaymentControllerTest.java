package com.stoliar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoliar.dto.PaymentRequest;
import com.stoliar.dto.PaymentResponse;
import com.stoliar.entity.enums.PaymentStatus;
import com.stoliar.service.PaymentService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
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

    @MockBean
    private PaymentService paymentService;

    @Test
    void createPayment_ValidRequest_ReturnsCreated() throws Exception {
        // Given
        String paymentId = new ObjectId().toString();
        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .userId(1L)
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(paymentId)
                .orderId(1L)
                .userId(1L)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(paymentId));
    }

    @Test
    void createPayment_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        PaymentRequest request = PaymentRequest.builder()
                .orderId(null) // Invalid
                .userId(1L)
                .paymentAmount(new BigDecimal("-100.00")) // Invalid
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPaymentById_Exists_ReturnsPayment() throws Exception {
        // Given
        String paymentId = new ObjectId().toString();
        PaymentResponse response = PaymentResponse.builder()
                .id(paymentId)
                .orderId(1L)
                .userId(1L)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        when(paymentService.getPaymentById(paymentId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/payments/{id}", paymentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(paymentId));
    }
}