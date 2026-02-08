package com.stoliar.service;

import com.stoliar.client.ExternalApiClient;
import com.stoliar.dto.PaymentRequest;
import com.stoliar.dto.PaymentResponse;
import com.stoliar.entity.Payment;
import com.stoliar.entity.enums.PaymentStatus;
import com.stoliar.mapper.PaymentMapper;
import com.stoliar.repository.PaymentRepository;
import com.stoliar.security.PaymentSecurity;
import com.stoliar.service.kafka.PaymentEventProducer;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceUnitTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private ExternalApiClient externalApiClient;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private PaymentSecurity paymentSecurity;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PaymentService paymentService;

    private Payment payment;
    private Payment payment2;
    private PaymentRequest paymentRequest;
    private PaymentResponse paymentResponse;
    private String paymentId;
    private Long userId;

    @BeforeEach
    void setUp() {
        paymentId = new ObjectId().toString();
        String paymentId2 = new ObjectId().toString();
        userId = 50L;

        payment = Payment.builder()
                .id(paymentId)
                .orderId(100L)
                .userId(userId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.of(2026, 1, 28, 14, 30, 0))
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        payment2 = Payment.builder()
                .id(paymentId2)
                .orderId(100L)
                .userId(userId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.of(2026, 1, 28, 16, 30, 0))
                .paymentAmount(new BigDecimal("200.25"))
                .build();

        paymentRequest = PaymentRequest.builder()
                .orderId(100L)
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        paymentResponse = PaymentResponse.builder()
                .id(paymentId)
                .orderId(100L)
                .userId(userId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("150.75"))
                .build();
    }

    @Test
    void createPayment_Success_AsUser() {
        // Given
        when(paymentSecurity.getCurrentUserId(authentication)).thenReturn(userId);
        when(paymentSecurity.isAdmin(authentication)).thenReturn(false);

        when(paymentMapper.toEntity(paymentRequest)).thenReturn(payment);
        when(externalApiClient.determinePaymentStatus()).thenReturn(PaymentStatus.COMPLETED);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        // When
        PaymentResponse result = paymentService.createPayment(paymentRequest, authentication, null);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        assertEquals(userId, result.getUserId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentEventProducer, times(1)).sendPaymentCreatedEvent(payment);
        verify(paymentSecurity, times(1)).getCurrentUserId(authentication);
        verify(paymentSecurity, times(1)).isAdmin(authentication);
    }

    @Test
    void createPayment_Success_AsAdminWithTargetUserId() {
        // Given
        Long adminId = 1L;
        Long targetUserId = 100L;

        when(paymentSecurity.isAdmin(authentication)).thenReturn(true);
        when(paymentSecurity.getCurrentUserId(authentication)).thenReturn(adminId);

        Payment targetPayment = Payment.builder()
                .id(paymentId)
                .orderId(100L)
                .userId(targetUserId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        PaymentResponse targetResponse = PaymentResponse.builder()
                .id(paymentId)
                .orderId(100L)
                .userId(targetUserId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        when(paymentMapper.toEntity(paymentRequest)).thenReturn(targetPayment);
        when(externalApiClient.determinePaymentStatus()).thenReturn(PaymentStatus.COMPLETED);
        when(paymentRepository.save(any(Payment.class))).thenReturn(targetPayment);
        when(paymentMapper.toResponse(targetPayment)).thenReturn(targetResponse);

        // When
        PaymentResponse result = paymentService.createPayment(paymentRequest, authentication, targetUserId);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        assertEquals(targetUserId, result.getUserId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void createPayment_Success_AsAdminWithoutTargetUserId() {
        // Given
        Long adminId = 1L;

        when(paymentSecurity.isAdmin(authentication)).thenReturn(true);
        when(paymentSecurity.getCurrentUserId(authentication)).thenReturn(adminId);

        Payment adminPayment = Payment.builder()
                .id(paymentId)
                .orderId(100L)
                .userId(adminId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        PaymentResponse adminResponse = PaymentResponse.builder()
                .id(paymentId)
                .orderId(100L)
                .userId(adminId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        when(paymentMapper.toEntity(paymentRequest)).thenReturn(adminPayment);
        when(externalApiClient.determinePaymentStatus()).thenReturn(PaymentStatus.COMPLETED);
        when(paymentRepository.save(any(Payment.class))).thenReturn(adminPayment);
        when(paymentMapper.toResponse(adminPayment)).thenReturn(adminResponse);

        // When
        PaymentResponse result = paymentService.createPayment(paymentRequest, authentication, null);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        assertEquals(adminId, result.getUserId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void createPayment_ExternalApiFailed() {
        // Given
        when(paymentSecurity.getCurrentUserId(authentication)).thenReturn(userId);
        when(paymentSecurity.isAdmin(authentication)).thenReturn(false);

        when(paymentMapper.toEntity(paymentRequest)).thenReturn(payment);
        when(externalApiClient.determinePaymentStatus()).thenReturn(PaymentStatus.FAILED);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        payment.setStatus(PaymentStatus.FAILED);
        paymentResponse.setStatus(PaymentStatus.FAILED);

        // When
        PaymentResponse result = paymentService.createPayment(paymentRequest, authentication, null);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentEventProducer, times(1)).sendPaymentCreatedEvent(payment);
    }

    @Test
    void getPaymentById_Success_AsUser() {
        // Given
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);
        when(paymentSecurity.checkPaymentAccess(paymentId, authentication)).thenReturn(true);

        // When
        PaymentResponse result = paymentService.getPaymentById(paymentId, authentication);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        assertEquals(userId, result.getUserId());
        verify(paymentRepository, times(1)).findById(paymentId);
        verify(paymentSecurity, times(1)).checkPaymentAccess(paymentId, authentication);
    }

    @Test
    void getPaymentById_Success_AsAdmin() {
        // Given
        Long otherUserId = 100L;

        Payment otherPayment = Payment.builder()
                .id(paymentId)
                .orderId(100L)
                .userId(otherUserId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        PaymentResponse otherResponse = PaymentResponse.builder()
                .id(paymentId)
                .orderId(100L)
                .userId(otherUserId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(otherPayment));
        when(paymentMapper.toResponse(otherPayment)).thenReturn(otherResponse);
        when(paymentSecurity.checkPaymentAccess(paymentId, authentication)).thenReturn(true);

        // When
        PaymentResponse result = paymentService.getPaymentById(paymentId, authentication);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        assertEquals(otherUserId, result.getUserId());
        verify(paymentRepository, times(1)).findById(paymentId);
    }

    @Test
    void getPaymentById_NotFound() {
        // Given
        String nonExistingId = new ObjectId().toString();
        when(paymentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.getPaymentById(nonExistingId, authentication));

        assertTrue(exception.getMessage().contains("Payment not found"));
        verify(paymentRepository, times(1)).findById(nonExistingId);
    }

    @Test
    void getPaymentsByUserId_Success_AsUser() {
        // Given
        List<Payment> payments = Arrays.asList(payment);
        when(paymentRepository.findByUserId(userId)).thenReturn(payments);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);
        when(paymentSecurity.checkUserAccess(userId, authentication)).thenReturn(true);

        // When
        List<PaymentResponse> results = paymentService.getPaymentsByUserId(userId, authentication);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(paymentId, results.get(0).getId());
        assertEquals(userId, results.get(0).getUserId());
        verify(paymentRepository, times(1)).findByUserId(userId);
        verify(paymentSecurity, times(1)).checkUserAccess(userId, authentication);
    }

    @Test
    void getPaymentsByUserId_Success_AsAdmin() {
        // Given
        Long targetUserId = 100L;

        Payment targetPayment = Payment.builder()
                .id(paymentId)
                .orderId(100L)
                .userId(targetUserId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        PaymentResponse targetResponse = PaymentResponse.builder()
                .id(paymentId)
                .orderId(100L)
                .userId(targetUserId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        List<Payment> payments = Arrays.asList(targetPayment);
        when(paymentRepository.findByUserId(targetUserId)).thenReturn(payments);
        when(paymentMapper.toResponse(targetPayment)).thenReturn(targetResponse);
        when(paymentSecurity.checkUserAccess(targetUserId, authentication)).thenReturn(true);

        // When
        List<PaymentResponse> results = paymentService.getPaymentsByUserId(targetUserId, authentication);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(paymentId, results.get(0).getId());
        assertEquals(targetUserId, results.get(0).getUserId());
        verify(paymentRepository, times(1)).findByUserId(targetUserId);
    }

    @Test
    void getPaymentsByOrderId_Success_AsUser() {
        // Given
        Long orderId = 100L;

        when(paymentSecurity.getCurrentUserId(authentication)).thenReturn(userId);
        when(paymentSecurity.isAdmin(authentication)).thenReturn(false);

        List<Payment> payments = Arrays.asList(payment);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(payments);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        // When
        List<PaymentResponse> results = paymentService.getPaymentsByOrderId(orderId, authentication);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(paymentId, results.get(0).getId());
        assertEquals(orderId, results.get(0).getOrderId());
        verify(paymentRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    void getPaymentsByOrderId_Success_AsAdmin() {
        // Given
        Long orderId = 100L;
        Long otherUserId = 200L;

        when(paymentSecurity.isAdmin(authentication)).thenReturn(true);

        Payment otherPayment = Payment.builder()
                .id(new ObjectId().toString())
                .orderId(orderId)
                .userId(otherUserId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("200.00"))
                .build();

        List<Payment> payments = Arrays.asList(payment, otherPayment);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(payments);
        when(paymentMapper.toResponse(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            return PaymentResponse.builder()
                    .id(p.getId())
                    .orderId(p.getOrderId())
                    .userId(p.getUserId())
                    .status(p.getStatus())
                    .timestamp(p.getTimestamp())
                    .paymentAmount(p.getPaymentAmount())
                    .build();
        });

        // When
        List<PaymentResponse> results = paymentService.getPaymentsByOrderId(orderId, authentication);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(paymentRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    void getPaymentsByOrderId_UserSeesOnlyOwnPayments() {
        // Given
        Long orderId = 100L;
        Long otherUserId = 200L;

        when(paymentSecurity.getCurrentUserId(authentication)).thenReturn(userId);
        when(paymentSecurity.isAdmin(authentication)).thenReturn(false);

        Payment otherPayment = Payment.builder()
                .id(new ObjectId().toString())
                .orderId(orderId)
                .userId(otherUserId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("200.00"))
                .build();

        List<Payment> allPayments = Arrays.asList(payment, otherPayment);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(allPayments);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        // When
        List<PaymentResponse> results = paymentService.getPaymentsByOrderId(orderId, authentication);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size()); // Только свой платеж
        assertEquals(userId, results.get(0).getUserId());
        verify(paymentRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    void getPaymentsByStatus_Success_AsUser() {
        // Given
        when(paymentSecurity.isAdmin(authentication)).thenReturn(false);
        when(paymentSecurity.getCurrentUserId(authentication)).thenReturn(userId);

        List<Payment> payments = Arrays.asList(payment);
        when(paymentRepository.findByStatus(PaymentStatus.COMPLETED)).thenReturn(payments);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        // When
        List<PaymentResponse> results = paymentService.getPaymentsByStatus(PaymentStatus.COMPLETED, authentication);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(PaymentStatus.COMPLETED, results.get(0).getStatus());
        assertEquals(userId, results.get(0).getUserId());
        verify(paymentRepository, times(1)).findByStatus(PaymentStatus.COMPLETED);
    }

    @Test
    void getPaymentsByCriteria_Success_AsUser() {
        // Given
        Long orderId = 100L;

        when(paymentSecurity.isAdmin(authentication)).thenReturn(false);
        when(paymentSecurity.getCurrentUserId(authentication)).thenReturn(userId);

        List<Payment> payments = Arrays.asList(payment);
        when(paymentRepository.findPaymentsByCriteria(userId, orderId, PaymentStatus.COMPLETED)).thenReturn(payments);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        // When
        List<PaymentResponse> results = paymentService.getPaymentsByCriteria(userId, orderId, PaymentStatus.COMPLETED, authentication);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(userId, results.get(0).getUserId());
        assertEquals(orderId, results.get(0).getOrderId());
        verify(paymentRepository, times(1)).findPaymentsByCriteria(userId, orderId, PaymentStatus.COMPLETED);
    }

    @Test
    void getPaymentsByCriteria_Success_AsAdmin() {
        // Given
        Long orderId = 100L;
        Long targetUserId = 200L;

        when(paymentSecurity.isAdmin(authentication)).thenReturn(true);

        Payment targetPayment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(targetUserId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        PaymentResponse targetResponse = PaymentResponse.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(targetUserId)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        List<Payment> payments = Arrays.asList(targetPayment);
        when(paymentRepository.findPaymentsByCriteria(targetUserId, orderId, PaymentStatus.COMPLETED)).thenReturn(payments);
        when(paymentMapper.toResponse(targetPayment)).thenReturn(targetResponse);

        // When
        List<PaymentResponse> results = paymentService.getPaymentsByCriteria(targetUserId, orderId, PaymentStatus.COMPLETED, authentication);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(targetUserId, results.get(0).getUserId());
        verify(paymentRepository, times(1)).findPaymentsByCriteria(targetUserId, orderId, PaymentStatus.COMPLETED);
    }

    @Test
    void getTotalSumByUserIdAndDateRange_Success() {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 28, 10, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 28, 18, 0, 0);

        when(paymentSecurity.checkUserAccess(userId, authentication)).thenReturn(true);

        List<Payment> payments = Arrays.asList(payment, payment2);
        when(paymentRepository.findByUserIdAndTimestampBetween(userId, startDate, endDate)).thenReturn(payments);

        // When
        BigDecimal result = paymentService.getTotalSumByUserIdAndDateRange(userId, startDate, endDate, authentication);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("351.00"), result);
        verify(paymentRepository, times(1)).findByUserIdAndTimestampBetween(userId, startDate, endDate);
        verify(paymentSecurity, times(1)).checkUserAccess(userId, authentication);
    }

    @Test
    void getTotalSumByDateRange_Success_AsAdmin() {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 28, 10, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 28, 18, 0, 0);

        when(paymentSecurity.isAdmin(authentication)).thenReturn(true);

        List<Payment> payments = Arrays.asList(payment, payment2);
        when(paymentRepository.findByTimestampBetween(startDate, endDate)).thenReturn(payments);

        // When
        BigDecimal result = paymentService.getTotalSumByDateRange(startDate, endDate, authentication);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("351.00"), result);
        verify(paymentRepository, times(1)).findByTimestampBetween(startDate, endDate);
        verify(paymentSecurity, times(1)).isAdmin(authentication);
    }

    @Test
    void getTotalSumByDateRange_ThrowsSecurityException_AsUser() {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 28, 10, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 28, 18, 0, 0);

        when(paymentSecurity.isAdmin(authentication)).thenReturn(false);

        // When & Then
        SecurityException exception = assertThrows(SecurityException.class,
                () -> paymentService.getTotalSumByDateRange(startDate, endDate, authentication));

        assertEquals("Admin only", exception.getMessage());
        verify(paymentSecurity, times(1)).isAdmin(authentication);
    }
}