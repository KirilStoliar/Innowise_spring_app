package com.stoliar.service.kafka;

import com.stoliar.dto.event.PaymentEvent;
import com.stoliar.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${spring.kafka.topics.payment-events}")
    private String paymentEventsTopic;

    public void sendPaymentCreatedEvent(Payment payment) {

        PaymentEvent event = PaymentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CREATE_PAYMENT")
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .status(payment.getStatus().name())
                .amount(payment.getPaymentAmount())
                .timestamp(payment.getTimestamp())
                .build();

        String key = String.valueOf(payment.getOrderId());

        kafkaTemplate.send(paymentEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info(
                                "PaymentEvent sent | topic={} partition={} offset={} key={} eventId={}",
                                result.getRecordMetadata().topic(), result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(), key, event.getEventId());
                    } else {
                        log.error("Failed to send PaymentEvent | key={} event={}", key, event, ex);
                    }
                });
    }
}

