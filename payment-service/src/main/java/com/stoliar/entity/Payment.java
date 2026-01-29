package com.stoliar.entity;

import com.stoliar.entity.enums.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
@CompoundIndexes({
        @CompoundIndex(name = "idx_order_user", def = "{'orderId': 1, 'userId': 1}"),
        @CompoundIndex(name = "idx_user_status", def = "{'userId': 1, 'status': 1}"),
        @CompoundIndex(name = "idx_order_status", def = "{'orderId': 1, 'status': 1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    private String id;

    @NotNull(message = "Order ID cannot be null")
    @Indexed
    @Field("orderId")
    private Long orderId;

    @NotNull(message = "User ID cannot be null")
    @Indexed
    @Field("userId")
    private Long userId;

    @NotNull(message = "Status cannot be null")
    @Indexed
    @Field("status")
    private PaymentStatus status;

    @PastOrPresent(message = "Timestamp must be in the past or present")
    @CreatedDate
    @Field("timestamp")
    private LocalDateTime timestamp;

    @NotNull(message = "Payment amount cannot be null")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    @Field("paymentAmount")
    private BigDecimal paymentAmount;
}