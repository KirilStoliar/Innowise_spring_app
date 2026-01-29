package com.stoliar.mapper;

import com.stoliar.dto.PaymentRequest;
import com.stoliar.dto.PaymentResponse;
import com.stoliar.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentMapper INSTANCE = Mappers.getMapper(PaymentMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    Payment toEntity(PaymentRequest paymentRequest);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "orderId", target = "orderId")
    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "timestamp", target = "timestamp")
    @Mapping(source = "paymentAmount", target = "paymentAmount")
    PaymentResponse toResponse(Payment payment);
}