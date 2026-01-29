package com.stoliar.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEvent {

    /** Уникальный ID события */
    private String eventId;

    private Long paymentId;
    private Long orderId;
    private Long userId;

    /** COMPLETED, FAILED, etc */
    private String status;

    private BigDecimal amount;
    private LocalDateTime timestamp;

    /** CREATE_PAYMENT, UPDATE_PAYMENT */
    private String eventType;
}
