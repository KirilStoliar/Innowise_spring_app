package com.stoliar.entity.enums;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED,
    DECLINED;

    public boolean isSuccess() {
        return this == COMPLETED;
    }

    public boolean isFailure() {
        return this == FAILED || this == DECLINED || this == CANCELLED;
    }
}