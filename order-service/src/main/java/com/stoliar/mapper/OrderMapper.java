package com.stoliar.mapper;

import com.stoliar.dto.order.OrderCreateDto;
import com.stoliar.dto.order.OrderResponseDto;
import com.stoliar.dto.user.UserInfoDto;
import com.stoliar.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ItemMapper.class})
public interface OrderMapper {
    
    @Mapping(target = "userEmail", source = "email")
    @Mapping(target = "userInfo", ignore = true)
    OrderResponseDto toResponseDto(Order order);
    
    List<OrderResponseDto> toResponseDtoList(List<Order> orders);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "orderCreateDto.userId")
    @Mapping(target = "email", source = "userInfo.email")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "totalPrice", expression = "java(0.0)")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    Order toEntity(OrderCreateDto orderCreateDto, UserInfoDto userInfo);
}