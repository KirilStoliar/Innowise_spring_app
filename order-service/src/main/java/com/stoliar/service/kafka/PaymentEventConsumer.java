package com.stoliar.service.kafka;

import com.stoliar.dto.event.PaymentEvent;
import com.stoliar.entity.Order;
import com.stoliar.exception.EntityNotFoundException;
import com.stoliar.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "${spring.kafka.topics.payment-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentEventKafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentEvent(
            @Payload PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {

        log.info("Received payment event: eventId={}, orderId={}, paymentId={}, status={}, eventType={}, partition={}, offset={}",
                event.getEventId(), event.getOrderId(), event.getPaymentId(), event.getStatus(), event.getEventType(), partition, offset);

        try {
            if (event.getOrderId() == null || event.getStatus() == null) {
                log.warn("Invalid PaymentEvent received, skipping. event={}", event);
                return;
            }

            if (!"CREATE_PAYMENT".equalsIgnoreCase(event.getEventType())) {
                log.debug("Skipping non-CREATE_PAYMENT event: {}", event.getEventType());
                return;
            }

            Order order = orderRepository.findByIdAndNotDeleted(event.getOrderId())
                    .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + event.getOrderId()));

            // Идемпотентная обработка статуса
            updateOrderStatusBasedOnPayment(order, event.getStatus());

            orderRepository.save(order);

            log.info("Order {} status updated to {} based on payment {} with status {}",
                    order.getId(), order.getStatus(), event.getPaymentId(), event.getStatus());

        } catch (EntityNotFoundException e) {
            log.warn("PaymentEvent ignored. Order not found. orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("Unexpected error while processing payment event: {}", event, e);
        } finally {
            // Всегда подтверждаем offset
            acknowledgment.acknowledge();
        }
    }

    private void updateOrderStatusBasedOnPayment(Order order, String paymentStatus) {
        if (paymentStatus == null) return;

        switch (paymentStatus.toUpperCase()) {
            case "COMPLETED":
                if (order.getStatus() == Order.OrderStatus.PENDING) {
                    order.setStatus(Order.OrderStatus.CONFIRMED);
                    log.info("Order {} confirmed due to successful payment", order.getId());
                } else {
                    log.info("Order {} already {}, skipping CONFIRMED update", order.getId(), order.getStatus());
                }
                break;

            case "FAILED":
            case "DECLINED":
                if (order.getStatus() == Order.OrderStatus.PENDING) {
                    order.setStatus(Order.OrderStatus.CANCELLED);
                    log.info("Order {} cancelled due to failed payment: {}", order.getId(), paymentStatus);
                } else {
                    log.info("Order {} already {}, skipping CANCELLED update", order.getId(), order.getStatus());
                }
                break;

            case "REFUNDED":
                if (order.getStatus() == Order.OrderStatus.CONFIRMED ||
                        order.getStatus() == Order.OrderStatus.SHIPPED) {
                    order.setStatus(Order.OrderStatus.CANCELLED);
                    log.info("Order {} cancelled due to refund", order.getId());
                }
                break;

            default:
                log.warn("Unhandled payment status: {} for order {}", paymentStatus, order.getId());
        }
    }
}