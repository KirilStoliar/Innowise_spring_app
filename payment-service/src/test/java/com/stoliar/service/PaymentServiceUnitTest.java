package com.stoliar.service;

import com.stoliar.client.ExternalApiClient;
import com.stoliar.dto.PaymentRequest;
import com.stoliar.dto.PaymentResponse;
import com.stoliar.entity.Payment;
import com.stoliar.entity.enums.PaymentStatus;
import com.stoliar.mapper.PaymentMapper;
import com.stoliar.repository.PaymentRepository;
import com.stoliar.service.kafka.PaymentEventProducer;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private ExternalApiClient externalApiClient;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentService paymentService;

    private Payment payment;
    private Payment payment2;
    private PaymentRequest paymentRequest;
    private PaymentResponse paymentResponse;
    private String paymentId;

    @BeforeEach
    void setUp() {
        paymentId = new ObjectId().toString();
        String paymentId2 = new ObjectId().toString();

        payment = Payment.builder()
                .id(paymentId)
                .orderId(100L)
                .userId(50L)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.of(2026, 1, 28, 14, 30, 0))
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        payment2 = Payment.builder()
                .id(paymentId2)
                .orderId(100L)
                .userId(50L)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.of(2026, 1, 28, 16, 30, 0))
                .paymentAmount(new BigDecimal("200.25"))
                .build();

        paymentRequest = PaymentRequest.builder()
                .orderId(100L)
                .userId(50L)
                .paymentAmount(new BigDecimal("150.75"))
                .build();

        paymentResponse = PaymentResponse.builder()
                .id(paymentId)
                .orderId(100L)
                .userId(50L)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("150.75"))
                .build();
    }

    @Test
    void createPayment_Success() {
        // Given
        when(paymentMapper.toEntity(paymentRequest)).thenReturn(payment);
        when(externalApiClient.determinePaymentStatus()).thenReturn(PaymentStatus.COMPLETED);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        // When
        PaymentResponse result = paymentService.createPayment(paymentRequest);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentEventProducer, times(1)).sendPaymentCreatedEvent(payment);
    }

    @Test
    void createPayment_ExternalApiFailed_Fallback() {
        // Given
        when(paymentMapper.toEntity(paymentRequest)).thenReturn(payment);
        when(externalApiClient.determinePaymentStatus()).thenReturn(PaymentStatus.FAILED);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);
        payment.setStatus(PaymentStatus.FAILED);

        // When
        PaymentResponse result = paymentService.createPayment(paymentRequest);

        // Then
        assertNotNull(result);
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentEventProducer, times(1)).sendPaymentCreatedEvent(payment);
    }

    @Test
    void getPaymentById_Success() {
        // Given
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        // When
        PaymentResponse result = paymentService.getPaymentById(paymentId);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        verify(paymentRepository, times(1)).findById(paymentId);
    }

    @Test
    void getPaymentById_NotFound() {
        // Given
        String nonExistingId = new ObjectId().toString();
        when(paymentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> paymentService.getPaymentById(nonExistingId));
        verify(paymentRepository, times(1)).findById(nonExistingId);
    }

    @Test
    void getPaymentsByUserId_Success() {
        // Given
        List<Payment> payments = Arrays.asList(payment);
        when(paymentRepository.findByUserId(50L)).thenReturn(payments);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        // When
        List<PaymentResponse> results = paymentService.getPaymentsByUserId(50L);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(paymentId, results.get(0).getId());
        verify(paymentRepository, times(1)).findByUserId(50L);
    }

    @Test
    void getPaymentsByOrderId_Success() {
        // Given
        List<Payment> payments = Arrays.asList(payment);
        when(paymentRepository.findByOrderId(100L)).thenReturn(payments);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        // When
        List<PaymentResponse> results = paymentService.getPaymentsByOrderId(100L);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(paymentId, results.get(0).getId());
        verify(paymentRepository, times(1)).findByOrderId(100L);
    }

    @Test
    void getPaymentsByStatus_Success() {
        // Given
        List<Payment> payments = Arrays.asList(payment);
        when(paymentRepository.findByStatus(PaymentStatus.COMPLETED)).thenReturn(payments);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        // When
        List<PaymentResponse> results = paymentService.getPaymentsByStatus(PaymentStatus.COMPLETED);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(paymentId, results.get(0).getId());
        verify(paymentRepository, times(1)).findByStatus(PaymentStatus.COMPLETED);
    }

    @Test
    void getPaymentsByCriteria_AllParameters() {
        // Given
        List<Payment> payments = Arrays.asList(payment);
        when(paymentRepository.findPaymentsByCriteria(50L, 100L, PaymentStatus.COMPLETED))
                .thenReturn(payments);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        // When
        List<PaymentResponse> results = paymentService.getPaymentsByCriteria(50L, 100L, PaymentStatus.COMPLETED);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(paymentId, results.get(0).getId());
    }

    @Test
    void getPaymentsByCriteria_NullParameters() {
        // Given
        List<Payment> payments = Arrays.asList(payment);
        when(paymentRepository.findPaymentsByCriteria(null, null, null))
                .thenReturn(payments);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        // When
        List<PaymentResponse> results = paymentService.getPaymentsByCriteria(null, null, null);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(paymentId, results.get(0).getId());
    }

    @Test
    void getTotalSumByUserIdAndDateRange_Success() {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 28, 10, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 28, 18, 0, 0);

        List<Payment> userPayments = Arrays.asList(payment, payment2);
        when(paymentRepository.findByUserId(50L)).thenReturn(userPayments);

        // When
        BigDecimal actual = paymentService.getTotalSumByUserIdAndDateRange(50L, startDate, endDate);

        // Then
        assertNotNull(actual);
        // payment: 150.75, payment2: 200.25
        // Сумма: 150.75 + 200.25 = 351.00
        assertEquals(new BigDecimal("351.00"), actual);
        verify(paymentRepository, times(1)).findByUserId(50L);
    }

    @Test
    void getTotalSumByUserIdAndDateRange_NoPaymentsInRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 29, 10, 0, 0); // Будущая дата
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 29, 18, 0, 0);

        List<Payment> userPayments = Arrays.asList(payment, payment2);
        when(paymentRepository.findByUserId(50L)).thenReturn(userPayments);

        // When
        BigDecimal actual = paymentService.getTotalSumByUserIdAndDateRange(50L, startDate, endDate);

        // Then
        assertNotNull(actual);
        assertEquals(BigDecimal.ZERO, actual);
        verify(paymentRepository, times(1)).findByUserId(50L);
    }

    @Test
    void getTotalSumByUserIdAndDateRange_EmptyUserPayments() {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 28, 10, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 28, 18, 0, 0);

        when(paymentRepository.findByUserId(50L)).thenReturn(Collections.emptyList());

        // When
        BigDecimal actual = paymentService.getTotalSumByUserIdAndDateRange(50L, startDate, endDate);

        // Then
        assertNotNull(actual);
        assertEquals(BigDecimal.ZERO, actual);
        verify(paymentRepository, times(1)).findByUserId(50L);
    }

    @Test
    void getTotalSumByDateRange_Success() {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 28, 10, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 28, 18, 0, 0);

        Payment payment3 = Payment.builder()
                .id(new ObjectId().toString())
                .orderId(200L)
                .userId(75L)
                .status(PaymentStatus.COMPLETED)
                .timestamp(LocalDateTime.of(2026, 1, 28, 15, 30, 0))
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        List<Payment> paymentsInRange = Arrays.asList(payment, payment2, payment3);
        when(paymentRepository.findByTimestampBetween(startDate, endDate)).thenReturn(paymentsInRange);

        // When
        BigDecimal actual = paymentService.getTotalSumByDateRange(startDate, endDate);

        // Then
        assertNotNull(actual);
        // payment: 150.75, payment2: 200.25, payment3: 100.00
        // Сумма: 150.75 + 200.25 + 100.00 = 451.00
        assertEquals(new BigDecimal("451.00"), actual);
        verify(paymentRepository, times(1)).findByTimestampBetween(startDate, endDate);
    }

    @Test
    void getTotalSumByDateRange_NoPaymentsInRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 29, 10, 0, 0); // Будущая дата
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 29, 18, 0, 0);

        when(paymentRepository.findByTimestampBetween(startDate, endDate)).thenReturn(Collections.emptyList());
        when(paymentRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        BigDecimal actual = paymentService.getTotalSumByDateRange(startDate, endDate);

        // Then
        assertNotNull(actual);
        assertEquals(BigDecimal.ZERO, actual);
        verify(paymentRepository, times(1)).findByTimestampBetween(startDate, endDate);
        verify(paymentRepository, times(1)).findAll();
    }

    @Test
    void getTotalSumByDateRange_FallbackToFindAll() {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2026, 1, 28, 10, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 28, 18, 0, 0);

        // findByTimestampBetween вернет пустой список
        when(paymentRepository.findByTimestampBetween(startDate, endDate)).thenReturn(Collections.emptyList());

        // findAll вернет все платежи (включая те, что вне диапазона)
        List<Payment> allPayments = Arrays.asList(payment, payment2);
        when(paymentRepository.findAll()).thenReturn(allPayments);

        // When
        BigDecimal actual = paymentService.getTotalSumByDateRange(startDate, endDate);

        // Then
        assertNotNull(actual);
        // payment и payment2 в диапазоне 10:00-18:00
        // payment.timestamp = 14:30, payment2.timestamp = 16:30
        assertEquals(new BigDecimal("351.00"), actual);
        verify(paymentRepository, times(1)).findByTimestampBetween(startDate, endDate);
        verify(paymentRepository, times(1)).findAll();
    }
}