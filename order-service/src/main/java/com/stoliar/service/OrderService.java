package com.stoliar.service;

import com.stoliar.dto.order.OrderCreateDto;
import com.stoliar.dto.order.OrderFilterDto;
import com.stoliar.dto.order.OrderResponseDto;
import com.stoliar.dto.order.OrderUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponseDto createOrder(OrderCreateDto orderCreateDto);
    OrderResponseDto getOrderById(Long id);
    Page<OrderResponseDto> getOrdersWithFilters(OrderFilterDto filterDto);
    Page<OrderResponseDto> getOrdersByUserId(Long userId, Pageable pageable);
    OrderResponseDto updateOrder(Long id, OrderUpdateDto orderUpdateDto);
    void deleteOrder(Long id);
}