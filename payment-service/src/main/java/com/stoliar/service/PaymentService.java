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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final ExternalApiClient externalApiClient;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentSecurity paymentSecurity;

    @Transactional
    public PaymentResponse createPayment(
            PaymentRequest paymentRequest,
            Authentication authentication,
            Long targetUserId
    ) {
        Long currentUserId = paymentSecurity.getCurrentUserId(authentication);
        boolean isAdmin = paymentSecurity.isAdmin(authentication);
        Long userIdToUse;

        if (isAdmin && targetUserId != null) {
            userIdToUse = targetUserId;
        } else {
            // USER всегда платит только за себя
            userIdToUse = currentUserId;
        }

        log.info("Creating payment: orderId={}, userId={}",
                paymentRequest.getOrderId(), userIdToUse);

        Payment payment = paymentMapper.toEntity(paymentRequest);
        payment.setUserId(userIdToUse);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTimestamp(LocalDateTime.now());

        PaymentStatus externalStatus = externalApiClient.determinePaymentStatus();
        payment.setStatus(externalStatus);

        Payment savedPayment = paymentRepository.save(payment);

        paymentEventProducer.sendPaymentCreatedEvent(savedPayment);

        return paymentMapper.toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String id, Authentication authentication) {
        log.info("Getting payment by id: {}", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        paymentSecurity.checkPaymentAccess(payment.getId(), authentication);
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUserId(Long userId, Authentication authentication) {
        log.info("Getting payments for userId: {}", userId);
        paymentSecurity.checkUserAccess(userId, authentication);
        return paymentRepository.findByUserId(userId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId, Authentication authentication) {
        log.info("Getting payments for orderId: {}", orderId);
        boolean isAdmin = paymentSecurity.isAdmin(authentication);
        Long currentUserId = paymentSecurity.getCurrentUserId(authentication);

        return paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> isAdmin || p.getUserId().equals(currentUserId))
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByStatus(PaymentStatus status, Authentication authentication) {
        log.info("Getting payments with status: {}", status);
        boolean isAdmin = paymentSecurity.isAdmin(authentication);
        Long userId = paymentSecurity.getCurrentUserId(authentication);

        return paymentRepository.findByStatus(status).stream()
                .filter(p -> isAdmin || p.getUserId().equals(userId))
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByCriteria(Long userId, Long orderId, PaymentStatus status,
                                                       Authentication authentication) {
        log.info("Getting payments by criteria - userId: {}, orderId: {}, status: {}",
                userId, orderId, status);
        boolean isAdmin = paymentSecurity.isAdmin(authentication);
        Long currentUserId = paymentSecurity.getCurrentUserId(authentication);

        if (!isAdmin) {
            userId = currentUserId;
        }

        return paymentRepository.findPaymentsByCriteria(userId, orderId, status).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSumByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate,
                                                      Authentication authentication) {
        log.info("Getting total sum for userId: {} from {} to {}", userId, startDate, endDate);

        paymentSecurity.checkUserAccess(userId, authentication);

        return paymentRepository.findByUserIdAndTimestampBetween(userId, startDate, endDate).stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSumByDateRange(LocalDateTime startDate, LocalDateTime endDate,
                                             Authentication authentication) {
        log.info("Getting total sum for all users from {} to {}", startDate, endDate);

        if (!paymentSecurity.isAdmin(authentication)) {
            throw new SecurityException("Admin only");
        }

        return paymentRepository.findByTimestampBetween(startDate, endDate).stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}