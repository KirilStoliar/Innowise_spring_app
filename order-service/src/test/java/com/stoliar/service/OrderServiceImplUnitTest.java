package com.stoliar.service;

import com.stoliar.client.UserServiceClient;
import com.stoliar.dto.order.OrderCreateDto;
import com.stoliar.dto.order.OrderResponseDto;
import com.stoliar.dto.order.OrderUpdateDto;
import com.stoliar.dto.orderItem.OrderItemCreateDto;
import com.stoliar.dto.orderItem.OrderItemDto;
import com.stoliar.dto.orderItem.OrderItemUpdateDto;
import com.stoliar.dto.user.UserInfoDto;
import com.stoliar.entity.Item;
import com.stoliar.entity.Order;
import com.stoliar.entity.OrderItem;
import com.stoliar.exception.EntityNotFoundException;
import com.stoliar.exception.ServiceUnavailableException;
import com.stoliar.mapper.OrderMapper;
import com.stoliar.mapper.ItemMapper;
import com.stoliar.repository.OrderRepository;
import com.stoliar.repository.OrderItemRepository;
import com.stoliar.repository.ItemRepository;
import com.stoliar.service.impl.OrderServiceImpl;
import com.stoliar.specification.OrderSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderSpecification orderSpecification;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    private Order testOrder;
    private Item testItem;
    private OrderItem testOrderItem;
    private UserInfoDto testUserInfo;
    private OrderResponseDto testOrderResponseDto;

    @BeforeEach
    void setUp() {
        // Setup test data
        testItem = new Item();
        testItem.setId(1L);
        testItem.setName("Test Item");
        testItem.setPrice(100.0);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(1L);
        testOrder.setEmail("test@example.com");
        testOrder.setStatus(Order.OrderStatus.PENDING);
        testOrder.setTotalPrice(200.0);
        testOrder.setDeleted(false);
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());

        testOrderItem = new OrderItem();
        testOrderItem.setId(1L);
        testOrderItem.setOrder(testOrder);
        testOrderItem.setItem(testItem);
        testOrderItem.setQuantity(2);
        testOrderItem.setCreatedAt(LocalDateTime.now());
        testOrderItem.setUpdatedAt(LocalDateTime.now());

        testOrder.setOrderItems(Arrays.asList(testOrderItem));

        testUserInfo = new UserInfoDto();
        testUserInfo.setId(1L);
        testUserInfo.setEmail("test@example.com");
        testUserInfo.setName("Test");
        testUserInfo.setSurname("User");
        testUserInfo.setActive(true);

        testOrderResponseDto = new OrderResponseDto();
        testOrderResponseDto.setId(1L);
        testOrderResponseDto.setUserId(1L);
        testOrderResponseDto.setUserEmail("test@example.com");
        testOrderResponseDto.setStatus(Order.OrderStatus.PENDING);
        testOrderResponseDto.setTotalPrice(200.0);
        testOrderResponseDto.setCreatedAt(LocalDateTime.now());
        testOrderResponseDto.setUpdatedAt(LocalDateTime.now());
        testOrderResponseDto.setUserInfo(testUserInfo);
    }

    @Test
    void createOrder_ValidData_ShouldReturnOrderResponse() {
        // Arrange
        OrderItemCreateDto orderItemDto = new OrderItemCreateDto();
        orderItemDto.setItemId(1L);
        orderItemDto.setQuantity(2);

        OrderCreateDto orderCreateDto = new OrderCreateDto();
        orderCreateDto.setUserId(1L);
        orderCreateDto.setOrderItems(Arrays.asList(orderItemDto));

        when(userServiceClient.getUserById(1L)).thenReturn(testUserInfo);
        when(orderMapper.toEntity(any(), any())).thenReturn(testOrder);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toResponseDto(any(Order.class))).thenReturn(testOrderResponseDto);

        OrderItemDto orderItemResponseDto = new OrderItemDto();
        orderItemResponseDto.setItemId(1L);
        orderItemResponseDto.setQuantity(2);
        orderItemResponseDto.setItemName("Test Item");
        orderItemResponseDto.setItemPrice(100.0);
        when(itemMapper.toDto(any(OrderItem.class))).thenReturn(orderItemResponseDto);

        // Act
        OrderResponseDto result = orderServiceImpl.createOrder(orderCreateDto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(Order.OrderStatus.PENDING, result.getStatus());
        assertEquals(200.0, result.getTotalPrice());
        assertNotNull(result.getUserInfo());
        assertEquals("test@example.com", result.getUserInfo().getEmail());

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(itemRepository, times(1)).findById(1L);
        verify(userServiceClient, times(1)).getUserById(1L);
    }

    @Test
    void createOrder_UserNotFound_ShouldCreateOrderWithFallback() {
        // Arrange
        UserInfoDto notFoundUser = new UserInfoDto();
        notFoundUser.setId(-1L);
        notFoundUser.setEmail("notfound@example.com");
        notFoundUser.setName("Not Found");
        notFoundUser.setSurname("User");
        notFoundUser.setActive(true);

        OrderItemCreateDto orderItemDto = new OrderItemCreateDto();
        orderItemDto.setItemId(1L);
        orderItemDto.setQuantity(2);

        OrderCreateDto orderCreateDto = new OrderCreateDto();
        orderCreateDto.setUserId(1L);
        orderCreateDto.setOrderItems(Arrays.asList(orderItemDto));

        // Настраиваем моки для сценария с fallback
        when(userServiceClient.getUserById(1L)).thenReturn(notFoundUser);
        when(orderMapper.toEntity(any(), any())).thenReturn(testOrder);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));

        // Мок для сохранения заказа
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUserId(1L);
        savedOrder.setEmail("service@unavailable.com");
        savedOrder.setStatus(Order.OrderStatus.PENDING);
        savedOrder.setTotalPrice(200.0);
        savedOrder.setDeleted(false);
        savedOrder.setCreatedAt(LocalDateTime.now());
        savedOrder.setUpdatedAt(LocalDateTime.now());
        savedOrder.setOrderItems(Arrays.asList(testOrderItem));

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Мок для маппера, который вернет fallback-ответ
        OrderResponseDto fallbackResponse = new OrderResponseDto();
        fallbackResponse.setId(1L);
        fallbackResponse.setUserId(1L);
        fallbackResponse.setUserEmail("service@unavailable.com");
        fallbackResponse.setStatus(Order.OrderStatus.PENDING);
        fallbackResponse.setTotalPrice(200.0);
        fallbackResponse.setCreatedAt(LocalDateTime.now());
        fallbackResponse.setUpdatedAt(LocalDateTime.now());

        UserInfoDto fallbackUser = new UserInfoDto();
        fallbackUser.setId(1L);
        fallbackUser.setEmail("service@unavailable.com");
        fallbackUser.setName("Service");
        fallbackUser.setSurname("Unavailable");
        fallbackUser.setActive(false);
        fallbackResponse.setUserInfo(fallbackUser);

        when(orderMapper.toResponseDto(any(Order.class))).thenReturn(fallbackResponse);

        OrderItemDto orderItemResponseDto = new OrderItemDto();
        orderItemResponseDto.setItemId(1L);
        orderItemResponseDto.setQuantity(2);
        orderItemResponseDto.setItemName("Test Item");
        orderItemResponseDto.setItemPrice(100.0);
        when(itemMapper.toDto(any(OrderItem.class))).thenReturn(orderItemResponseDto);

        // Act
        OrderResponseDto result = orderServiceImpl.createOrder(orderCreateDto);

        // Assert - проверяем, что заказ создан с fallback-пользователем
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("service@unavailable.com", result.getUserEmail());
        assertNotNull(result.getUserInfo());
        assertEquals("service@unavailable.com", result.getUserInfo().getEmail());
        assertEquals("Service", result.getUserInfo().getName());
        assertEquals("Unavailable", result.getUserInfo().getSurname());
        assertFalse(result.getUserInfo().getActive());
    }

    @Test
    void createOrder_ItemNotFound_ShouldThrowEntityNotFoundException() {
        // Arrange
        OrderItemCreateDto orderItemDto = new OrderItemCreateDto();
        orderItemDto.setItemId(999L);
        orderItemDto.setQuantity(2);

        OrderCreateDto orderCreateDto = new OrderCreateDto();
        orderCreateDto.setUserId(1L);
        orderCreateDto.setOrderItems(List.of(orderItemDto));

        when(userServiceClient.getUserById(1L)).thenReturn(testUserInfo);
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());
        when(orderMapper.toEntity(any(), any())).thenReturn(testOrder);

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> orderServiceImpl.createOrder(orderCreateDto));
    }

    @Test
    void getOrderById_UserServiceUnavailable_ShouldThrowServiceUnavailableException() {
        // Arrange
        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testOrder));
        when(userServiceClient.getUserById(1L)).thenThrow(new RuntimeException("Service unavailable"));

        // Act & Assert
        assertThrows(ServiceUnavailableException.class, () ->
                orderServiceImpl.getOrderById(1L));
    }

    @Test
    void getOrderById_ExistingOrder_ShouldReturnOrderResponse() {
        // Arrange
        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testOrder));
        when(userServiceClient.getUserById(1L)).thenReturn(testUserInfo);
        when(orderMapper.toResponseDto(testOrder)).thenReturn(testOrderResponseDto);

        OrderItemDto orderItemResponseDto = new OrderItemDto();
        orderItemResponseDto.setItemId(1L);
        orderItemResponseDto.setQuantity(2);
        orderItemResponseDto.setItemName("Test Item");
        orderItemResponseDto.setItemPrice(100.0);
        when(itemMapper.toDto(any(OrderItem.class))).thenReturn(orderItemResponseDto);

        // Act
        OrderResponseDto result = orderServiceImpl.getOrderById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(testUserInfo, result.getUserInfo());
        verify(orderRepository, times(1)).findByIdAndNotDeleted(1L);
        verify(userServiceClient, times(1)).getUserById(1L);
    }

    @Test
    void getOrderById_NonExistingOrder_ShouldThrowEntityNotFoundException() {
        // Arrange
        when(orderRepository.findByIdAndNotDeleted(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
            orderServiceImpl.getOrderById(999L));
    }

    @Test
    void getOrdersWithFilters_ValidFilters_ShouldReturnPageOfOrders() {
        // Arrange
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));
        Pageable pageable = PageRequest.of(0, 10);

        when(orderSpecification.withFilters(any(), any(), any()))
            .thenReturn((Specification<Order>) (root, query, criteriaBuilder) -> null);
        when(orderRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(orderPage);
        when(userServiceClient.getUserById(1L)).thenReturn(testUserInfo);
        when(orderMapper.toResponseDto(testOrder)).thenReturn(testOrderResponseDto);

        OrderItemDto orderItemResponseDto = new OrderItemDto();
        orderItemResponseDto.setItemId(1L);
        orderItemResponseDto.setQuantity(2);
        orderItemResponseDto.setItemName("Test Item");
        orderItemResponseDto.setItemPrice(100.0);
        when(itemMapper.toDto(any(OrderItem.class))).thenReturn(orderItemResponseDto);

        // Act
        Page<OrderResponseDto> result = orderServiceImpl.getOrdersWithFilters(
            new com.stoliar.dto.order.OrderFilterDto());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(orderRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getOrdersByUserId_ValidUserId_ShouldReturnPageOfOrders() {
        // Arrange
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));
        Pageable pageable = PageRequest.of(0, 10);

        when(orderRepository.findByUserId(1L, pageable)).thenReturn(orderPage);
        when(userServiceClient.getUserById(1L)).thenReturn(testUserInfo);
        when(orderMapper.toResponseDto(testOrder)).thenReturn(testOrderResponseDto);

        OrderItemDto orderItemResponseDto = new OrderItemDto();
        orderItemResponseDto.setItemId(1L);
        orderItemResponseDto.setQuantity(2);
        orderItemResponseDto.setItemName("Test Item");
        orderItemResponseDto.setItemPrice(100.0);
        when(itemMapper.toDto(any(OrderItem.class))).thenReturn(orderItemResponseDto);

        // Act
        Page<OrderResponseDto> result = orderServiceImpl.getOrdersByUserId(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(orderRepository, times(1)).findByUserId(1L, pageable);
    }

    @Test
    void updateOrder_ValidUpdate_ShouldReturnUpdatedOrder() {
        // Arrange
        OrderUpdateDto updateDto = new OrderUpdateDto();
        updateDto.setStatus(Order.OrderStatus.CONFIRMED);

        testOrderResponseDto.setStatus(Order.OrderStatus.CONFIRMED);

        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(userServiceClient.getUserById(1L)).thenReturn(testUserInfo);
        when(orderMapper.toResponseDto(testOrder)).thenReturn(testOrderResponseDto);

        OrderItemDto orderItemResponseDto = new OrderItemDto();
        orderItemResponseDto.setItemId(1L);
        orderItemResponseDto.setQuantity(2);
        orderItemResponseDto.setItemName("Test Item");
        orderItemResponseDto.setItemPrice(100.0);
        when(itemMapper.toDto(any(OrderItem.class))).thenReturn(orderItemResponseDto);

        // Act
        OrderResponseDto result = orderServiceImpl.updateOrder(1L, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.CONFIRMED, result.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void updateOrder_UpdateItemsInInvalidStatus_ShouldThrowIllegalStateException() {
        // Arrange
        testOrder.setStatus(Order.OrderStatus.SHIPPED);
        OrderUpdateDto updateDto = new OrderUpdateDto();
        updateDto.setStatus(Order.OrderStatus.SHIPPED);

        // Добавляем товары для обновления
        OrderItemUpdateDto orderItemUpdateDto = new OrderItemUpdateDto();
        orderItemUpdateDto.setItemId(1L);
        orderItemUpdateDto.setQuantity(3);
        updateDto.setOrderItems(Arrays.asList(orderItemUpdateDto));

        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            orderServiceImpl.updateOrder(1L, updateDto));
    }

    @Test
    void updateOrder_ChangeUserInInvalidStatus_ShouldThrowIllegalStateException() {
        // Arrange
        testOrder.setStatus(Order.OrderStatus.SHIPPED);
        OrderUpdateDto updateDto = new OrderUpdateDto();
        updateDto.setStatus(Order.OrderStatus.SHIPPED);
        updateDto.setUserId(2L); // Пытаемся сменить пользователя

        when(orderRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            orderServiceImpl.updateOrder(1L, updateDto));
    }

    @Test
    void deleteOrder_ExistingOrder_ShouldSoftDelete() {
        // Arrange
        when(orderRepository.existsByIdAndNotDeleted(1L)).thenReturn(true);
        doNothing().when(orderItemRepository).deleteByOrderId(1L);
        doNothing().when(orderRepository).softDeleteById(1L);

        // Act
        orderServiceImpl.deleteOrder(1L);

        // Assert
        verify(orderRepository, times(1)).softDeleteById(1L);
        verify(orderItemRepository, times(1)).deleteByOrderId(1L);
    }

    @Test
    void deleteOrder_NonExistingOrder_ShouldThrowEntityNotFoundException() {
        // Arrange
        when(orderRepository.existsByIdAndNotDeleted(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
            orderServiceImpl.deleteOrder(999L));
    }
}