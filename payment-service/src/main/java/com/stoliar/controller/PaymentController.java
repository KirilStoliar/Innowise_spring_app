package com.stoliar.controller;

import com.stoliar.dto.ApiResponse;
import com.stoliar.dto.PaymentRequest;
import com.stoliar.dto.PaymentResponse;
import com.stoliar.entity.enums.PaymentStatus;
import com.stoliar.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "APIs for managing payments")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @Operation(summary = "Create payment", description = "Create a new payment with status determined by external API")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@Valid @RequestBody PaymentRequest paymentRequest) {
        log.info("Creating payment for order: {}, user: {}", 
                paymentRequest.getOrderId(), paymentRequest.getUserId());
        
        PaymentResponse payment = paymentService.createPayment(paymentRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment, "Payment created successfully"));
    }
    
    @Operation(summary = "Get payment by ID", description = "Retrieve a specific payment by ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @Parameter(description = "Payment ID", required = true) @PathVariable String id) {
        
        log.info("Getting payment by id: {}", id);
        PaymentResponse payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment retrieved successfully"));
    }
    
    @Operation(summary = "Get payments by user ID", description = "Retrieve all payments for a specific user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByUserId(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {
        
        log.info("Getting payments for user id: {}", userId);
        List<PaymentResponse> payments = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(payments, "Payments retrieved successfully"));
    }
    
    @Operation(summary = "Get payments by order ID", description = "Retrieve all payments for a specific order")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByOrderId(
            @Parameter(description = "Order ID", required = true) @PathVariable Long orderId) {
        
        log.info("Getting payments for order id: {}", orderId);
        List<PaymentResponse> payments = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(payments, "Payments retrieved successfully"));
    }
    
    @Operation(summary = "Get payments by status", description = "Retrieve all payments with specific status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByStatus(
            @Parameter(description = "Payment status", required = true) @PathVariable PaymentStatus status) {
        
        log.info("Getting payments with status: {}", status);
        List<PaymentResponse> payments = paymentService.getPaymentsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(payments, "Payments retrieved successfully"));
    }
    
    @Operation(summary = "Get payments by criteria", description = "Retrieve payments filtered by user ID, order ID, and/or status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByCriteria(
            @Parameter(description = "User ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "Order ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "Payment status") @RequestParam(required = false) PaymentStatus status) {
        
        log.info("Searching payments - userId: {}, orderId: {}, status: {}", userId, orderId, status);
        List<PaymentResponse> payments = paymentService.getPaymentsByCriteria(userId, orderId, status);
        return ResponseEntity.ok(ApiResponse.success(payments, "Payments retrieved successfully"));
    }

    @Operation(summary = "Get total sum for user", description = "Get total payment amount for a user within date range")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Total sum calculated successfully")
    })
    @GetMapping("/user/{userId}/total")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalSumByUserId(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId,
            @Parameter(description = "Start date (ISO format). Example: 2026-01-28T10:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format). Example: 2026-01-28T18:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Calculating total sum for user {} from {} to {}", userId, startDate, endDate);

        BigDecimal totalSum = paymentService.getTotalSumByUserIdAndDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(totalSum, "Total sum calculated successfully"));
    }

    @Operation(summary = "Get total sum for all users", description = "Get total payment amount for all users within date range")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Total sum calculated successfully")
    })
    @GetMapping("/total")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalSum(
            @Parameter(description = "Start date (ISO format). Example: 2026-01-28T10:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format). Example: 2026-01-28T18:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Calculating total sum for all users from {} to {}", startDate, endDate);

        BigDecimal totalSum = paymentService.getTotalSumByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(totalSum, "Total sum calculated successfully"));
    }
}