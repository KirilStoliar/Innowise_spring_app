package com.stoliar.service;

import com.stoliar.client.ExternalApiClient;
import com.stoliar.dto.PaymentRequest;
import com.stoliar.dto.PaymentResponse;
import com.stoliar.entity.Payment;
import com.stoliar.entity.enums.PaymentStatus;
import com.stoliar.mapper.PaymentMapper;
import com.stoliar.repository.PaymentRepository;
import com.stoliar.service.kafka.PaymentEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    public PaymentResponse createPayment(PaymentRequest paymentRequest) {
        log.info("Creating payment for orderId: {}, userId: {}",
                paymentRequest.getOrderId(), paymentRequest.getUserId());

        // Создаем платеж с PENDING статусом
        Payment payment = paymentMapper.toEntity(paymentRequest);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTimestamp(LocalDateTime.now());

        // Определяем статус через внешний API
        PaymentStatus externalStatus = externalApiClient.determinePaymentStatus();
        payment.setStatus(externalStatus);

        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment created with id: {}, status: {}",
                savedPayment.getId(), savedPayment.getStatus());

        // Отправляем событие в Kafka
        paymentEventProducer.sendPaymentCreatedEvent(savedPayment);

        return paymentMapper.toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String id) {
        log.info("Getting payment by id: {}", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        log.info("Getting payments for userId: {}", userId);
        return paymentRepository.findByUserId(userId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        log.info("Getting payments for orderId: {}", orderId);
        return paymentRepository.findByOrderId(orderId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByStatus(PaymentStatus status) {
        log.info("Getting payments with status: {}", status);
        return paymentRepository.findByStatus(status).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByCriteria(Long userId, Long orderId, PaymentStatus status) {
        log.info("Getting payments by criteria - userId: {}, orderId: {}, status: {}",
                userId, orderId, status);
        return paymentRepository.findPaymentsByCriteria(userId, orderId, status).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSumByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Getting total sum for userId: {} from {} to {}", userId, startDate, endDate);

        List<Payment> userPayments = paymentRepository.findByUserId(userId);
        log.debug("Found {} total payments for user {}", userPayments.size(), userId);

        List<Payment> filteredPayments = userPayments.stream()
                .filter(payment -> {
                    LocalDateTime timestamp = payment.getTimestamp();
                    boolean isInRange = !timestamp.isBefore(startDate) && !timestamp.isAfter(endDate);

                    if (isInRange) {
                        log.debug("Payment in range: id={}, amount={}, timestamp={}",
                                payment.getId(), payment.getPaymentAmount(), timestamp);
                    }

                    return isInRange;
                })
                .collect(Collectors.toList());

        log.info("Found {} payments in date range for user {}", filteredPayments.size(), userId);

        BigDecimal total = filteredPayments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total sum for user {}: {}", userId, total);
        return total;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSumByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Getting total sum for all users from {} to {}", startDate, endDate);

        List<Payment> allPayments = paymentRepository.findByTimestampBetween(startDate, endDate);

        if (allPayments.isEmpty()) {
            allPayments = paymentRepository.findAll().stream()
                    .filter(payment -> {
                        LocalDateTime timestamp = payment.getTimestamp();
                        return !timestamp.isBefore(startDate) && !timestamp.isAfter(endDate);
                    })
                    .collect(Collectors.toList());
        }

        log.info("Found {} payments in date range", allPayments.size());

        allPayments.forEach(payment ->
                log.debug("Payment in range: id={}, userId={}, amount={}, timestamp={}",
                        payment.getId(), payment.getUserId(), payment.getPaymentAmount(), payment.getTimestamp()));

        BigDecimal total = allPayments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total sum for all users: {}", total);
        return total;
    }
}