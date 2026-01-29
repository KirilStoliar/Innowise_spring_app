package com.stoliar.service.impl;

import com.stoliar.client.UserServiceClient;
import com.stoliar.dto.orderItem.OrderItemCreateDto;
import com.stoliar.dto.orderItem.OrderItemDto;
import com.stoliar.dto.order.OrderCreateDto;
import com.stoliar.dto.order.OrderFilterDto;
import com.stoliar.dto.order.OrderResponseDto;
import com.stoliar.dto.order.OrderUpdateDto;
import com.stoliar.dto.orderItem.OrderItemUpdateDto;
import com.stoliar.dto.user.UserInfoDto;
import com.stoliar.entity.Order;
import com.stoliar.entity.OrderItem;
import com.stoliar.entity.Item;
import com.stoliar.exception.EntityNotFoundException;
import com.stoliar.exception.ServiceUnavailableException;
import com.stoliar.mapper.OrderMapper;
import com.stoliar.mapper.ItemMapper;
import com.stoliar.repository.OrderRepository;
import com.stoliar.repository.OrderItemRepository;
import com.stoliar.repository.ItemRepository;
import com.stoliar.service.OrderService;
import com.stoliar.specification.OrderSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final OrderSpecification orderSpecification;
    private final OrderMapper orderMapper;
    private final ItemMapper itemMapper;
    private final UserServiceClient userServiceClient;

    @Transactional
    public OrderResponseDto createOrder(OrderCreateDto dto) {

        log.info("Creating order for user id: {}", dto.getUserId());

        UserInfoDto userInfo;

        try {
            userInfo = userServiceClient.getUserById(dto.getUserId());
        } catch (Exception e) {
            log.error("User service unavailable", e);
            throw new ServiceUnavailableException("User service unavailable", e);
        }

        // fallback только если пользователь НЕ НАЙДЕН
        if (userInfo.getId() == null || userInfo.getId() == -1L) {
            userInfo = createFallbackUser(dto.getUserId());
        }

        Order order = orderMapper.toEntity(dto, userInfo);

        createOrderItems(order, dto.getOrderItems());
        calculateTotalPrice(order);

        Order saved = orderRepository.save(order);

        return enrichOrderWithUserInfo(saved, userInfo);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        log.info("Getting order by id: {}", id);

        Order order = orderRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        try {
            UserInfoDto userInfo = userServiceClient.getUserById(order.getUserId());
            return enrichOrderWithUserInfo(order, userInfo);
        } catch (Exception e) {
            log.error("Failed to get user info for order {}: {}", id, e.getMessage());
            throw new ServiceUnavailableException("Failed to retrieve order details", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrdersWithFilters(OrderFilterDto filterDto) {
        log.info("Getting orders with filters");

        Pageable pageable = PageRequest.of(filterDto.getPage(), filterDto.getSize());
        Specification<Order> spec = orderSpecification.withFilters(
                filterDto.getCreatedFrom(), filterDto.getCreatedTo(), filterDto.getStatuses());

        Page<Order> ordersPage = orderRepository.findAll(spec, pageable);

        return ordersPage.map(order -> {
            try {
                UserInfoDto userInfo = userServiceClient.getUserById(order.getUserId());
                return enrichOrderWithUserInfo(order, userInfo);
            } catch (Exception e) {
                log.warn("Failed to get user info for order {}: {}", order.getId(), e.getMessage());
                return enrichOrderWithUserInfo(order, createFallbackUser(order.getUserId()));
            }
        });
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrdersByUserId(Long userId, Pageable pageable) {
        log.info("Getting orders for user: {}", userId);

        Page<Order> ordersPage = orderRepository.findByUserId(userId, pageable);

        try {
            UserInfoDto userInfo = userServiceClient.getUserById(userId);
            return ordersPage.map(order -> enrichOrderWithUserInfo(order, userInfo));
        } catch (Exception e) {
            log.warn("Failed to get user info for user {}: {}", userId, e.getMessage());
            UserInfoDto fallbackUser = createFallbackUser(userId);
            return ordersPage.map(order -> enrichOrderWithUserInfo(order, fallbackUser));
        }
    }

    @Transactional
    public OrderResponseDto updateOrder(Long id, OrderUpdateDto orderUpdateDto) {
        log.info("Updating order with id: {}", id);

        Order existingOrder = orderRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        // Проверяем, можно ли изменять заказ в текущем статусе
        validateOrderStatusForUpdate(existingOrder.getStatus(), orderUpdateDto);

        // Обновляем статус
        existingOrder.setStatus(orderUpdateDto.getStatus());

        // Если передан userId - обновляем пользователя
        if (orderUpdateDto.getUserId() != null) {
            validateUserForOrderUpdate(existingOrder, orderUpdateDto.getUserId());
            UserInfoDto userInfo = getUserInfo(orderUpdateDto.getUserId());
            existingOrder.setUserId(orderUpdateDto.getUserId());
            existingOrder.setEmail(userInfo.getEmail());
        }

        // Если передан список товаров - обновляем состав заказа
        if (orderUpdateDto.getOrderItems() != null && !orderUpdateDto.getOrderItems().isEmpty()) {
            updateOrderItems(existingOrder, orderUpdateDto.getOrderItems());
            calculateTotalPrice(existingOrder);
        }

        Order updatedOrder = orderRepository.save(existingOrder);

        try {
            UserInfoDto userInfo = userServiceClient.getUserById(updatedOrder.getUserId());
            return enrichOrderWithUserInfo(updatedOrder, userInfo);
        } catch (Exception e) {
            log.error("Failed to get user info for updated order {}: {}", id, e.getMessage());
            throw new ServiceUnavailableException("Failed to update order", e);
        }
    }

    @Transactional
    public void deleteOrder(Long id) {
        log.info("Deleting order with id: {}", id);

        if (!orderRepository.existsByIdAndNotDeleted(id)) {
            throw new EntityNotFoundException("Order not found with id: " + id);
        }

        orderItemRepository.deleteByOrderId(id);
        orderRepository.softDeleteById(id);
    }

    private void createOrderItems(Order order, List<OrderItemCreateDto> orderItemCreateDtos) {
        if (orderItemCreateDtos == null || orderItemCreateDtos.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        List<OrderItem> orderItems = orderItemCreateDtos.stream()
                .map(dto -> {
                    Item item = itemRepository.findById(dto.getItemId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Item not found with id: " + dto.getItemId()));

                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setItem(item);
                    orderItem.setQuantity(dto.getQuantity());
                    return orderItem;
                })
                .collect(Collectors.toList());

        order.setOrderItems(orderItems);
    }

    private void calculateTotalPrice(Order order) {
        double total = order.getOrderItems().stream()
                .mapToDouble(item -> item.getItem().getPrice() * item.getQuantity())
                .sum();

        order.setTotalPrice(total);
    }

    private OrderResponseDto enrichOrderWithUserInfo(Order order, UserInfoDto userInfo) {
        OrderResponseDto responseDto = orderMapper.toResponseDto(order);
        responseDto.setUserInfo(userInfo);

        if (order.getOrderItems() != null) {
            List<OrderItemDto> enrichedItems = order.getOrderItems().stream()
                    .map(itemMapper::toDto)
                    .collect(Collectors.toList());
            responseDto.setOrderItems(enrichedItems);
        }

        return responseDto;
    }

    private UserInfoDto createFallbackUser(Long userId) {
        UserInfoDto fallback = new UserInfoDto();
        fallback.setId(userId);
        fallback.setEmail("service@unavailable.com");
        fallback.setName("Service");
        fallback.setSurname("Unavailable");
        fallback.setActive(false);
        return fallback;
    }

    private void validateOrderStatusForUpdate(Order.OrderStatus currentStatus, OrderUpdateDto updateDto) {
        // Определяем, в каких статусах можно изменять состав заказа
        List<Order.OrderStatus> allowedStatusesForItemUpdate = Arrays.asList(
                Order.OrderStatus.PENDING,
                Order.OrderStatus.CONFIRMED
        );

        // Если пытаемся изменить товары в неподходящем статусе
        if (updateDto.getOrderItems() != null &&
                !updateDto.getOrderItems().isEmpty() &&
                !allowedStatusesForItemUpdate.contains(currentStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot update order items in current status: %s. " +
                                    "Allowed statuses: %s",
                            currentStatus, allowedStatusesForItemUpdate)
            );
        }

        // Если пытаемся изменить пользователя в неподходящем статусе
        if (updateDto.getUserId() != null && !allowedStatusesForItemUpdate.contains(currentStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot change user for order in current status: %s. " +
                                    "Allowed statuses: %s",
                            currentStatus, allowedStatusesForItemUpdate)
            );
        }
    }

    private void updateOrderItems(Order order, List<OrderItemUpdateDto> orderItemUpdateDtos) {
        // Удаляем старые элементы
        orderItemRepository.deleteByOrderId(order.getId());

        // Создаем новые элементы
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemUpdateDto dto : orderItemUpdateDtos) {
            Item item = itemRepository.findById(dto.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Item not found with id: " + dto.getItemId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItem(item);
            orderItem.setQuantity(dto.getQuantity());

            // Сохраняем сразу
            OrderItem savedOrderItem = orderItemRepository.save(orderItem);
            orderItems.add(savedOrderItem);
        }

        order.setOrderItems(orderItems);
    }

    private void validateUserForOrderUpdate(Order order, Long newUserId) {

        List<Order.OrderStatus> allowedStatusesForUserChange = Arrays.asList(
                Order.OrderStatus.PENDING,
                Order.OrderStatus.CONFIRMED
        );

        if (!allowedStatusesForUserChange.contains(order.getStatus())) {
            throw new IllegalStateException(
                    String.format("Cannot change user for order in current status: %s. " +
                                    "Allowed statuses: %s",
                            order.getStatus(), allowedStatusesForUserChange)
            );
        }
    }

    private UserInfoDto getUserInfo(Long userId) {
        try {
            UserInfoDto userInfo = userServiceClient.getUserById(userId);

            // Проверяем, что пользователь найден и активен
            if (userInfo.getId() == null || userInfo.getId() == -1L) {
                throw new EntityNotFoundException("User not found with id: " + userId);
            }

            if (userInfo.getActive() != null && !userInfo.getActive()) {
                log.warn("User {} is inactive, but order is being updated", userId);
            }

            return userInfo;
        } catch (Exception e) {
            log.error("Failed to get user info for userId {}: {}", userId, e.getMessage());
            throw new ServiceUnavailableException("Failed to get user information", e);
        }
    }
}