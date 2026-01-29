package com.stoliar.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stoliar.entity.enums.PaymentStatus;
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
public class PaymentResponse {
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String id;
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long orderId;
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long userId;
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private PaymentStatus status;
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal paymentAmount;
}