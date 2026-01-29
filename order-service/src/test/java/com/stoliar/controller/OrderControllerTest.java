package com.stoliar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoliar.config.TestSecurityConfig;
import com.stoliar.dto.order.OrderCreateDto;
import com.stoliar.dto.order.OrderResponseDto;
import com.stoliar.dto.orderItem.OrderItemCreateDto;
import com.stoliar.entity.Order;
import com.stoliar.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderServiceImpl;

    @Test
    void createOrder_ValidRequest_ShouldReturnCreated() throws Exception {
        // Arrange
        OrderItemCreateDto itemDto = new OrderItemCreateDto();
        itemDto.setItemId(1L);
        itemDto.setQuantity(2);

        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setUserId(1L);
        createDto.setOrderItems(Arrays.asList(itemDto));

        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setId(1L);
        responseDto.setUserId(1L);
        responseDto.setStatus(Order.OrderStatus.PENDING);

        when(orderServiceImpl.createOrder(any(OrderCreateDto.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    void getOrderById_ExistingOrder_ShouldReturnOrder() throws Exception {
        // Arrange
        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setId(1L);
        responseDto.setUserId(1L);
        responseDto.setStatus(Order.OrderStatus.PENDING);

        when(orderServiceImpl.getOrderById(1L)).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getOrdersByUserId_ShouldReturnPaginatedResults() throws Exception {
        // Arrange
        OrderResponseDto order1 = new OrderResponseDto();
        order1.setId(1L);
        order1.setUserId(1L);

        OrderResponseDto order2 = new OrderResponseDto();
        order2.setId(2L);
        order2.setUserId(1L);

        Page<OrderResponseDto> page = new PageImpl<>(
            Arrays.asList(order1, order2),
            PageRequest.of(0, 10),
            2
        );

        when(orderServiceImpl.getOrdersByUserId(eq(1L), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/user/1")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void deleteOrder_ShouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(orderServiceImpl).deleteOrder(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/orders/1"))
                .andExpect(status().isNoContent());
    }
}