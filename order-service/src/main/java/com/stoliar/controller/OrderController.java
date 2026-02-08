package com.stoliar.controller;

import com.stoliar.dto.order.OrderCreateDto;
import com.stoliar.dto.order.OrderFilterDto;
import com.stoliar.dto.order.OrderResponseDto;
import com.stoliar.dto.order.OrderUpdateDto;
import com.stoliar.entity.Order;
import com.stoliar.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing orders")
public class OrderController {

    private final OrderService orderServiceImpl;

    @Operation(summary = "Create a new order", description = "Create a new order with items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "User or item not found")
    })
    @PreAuthorize("hasRole('ADMIN') or #orderCreateDto.userId == authentication.principal")
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderCreateDto orderCreateDto) {
        log.info("Creating new order for user: {}", orderCreateDto.getUserId());
        OrderResponseDto createdOrder = orderServiceImpl.createOrder(orderCreateDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @Operation(summary = "Get order by ID", description = "Retrieve a specific order by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.checkOrderAccess(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @Parameter(description = "Order ID", required = true) @PathVariable Long id) {
        
        log.info("Getting order by id: {}", id);
        OrderResponseDto order = orderServiceImpl.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Get all orders with filters", description = "Retrieve paginated list of orders with optional filters by date range and status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<OrderResponseDto>> getOrdersWithFilters(
            @Parameter(description = "Filter by created from date (ISO format)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            
            @Parameter(description = "Filter by created to date (ISO format)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            
            @Parameter(description = "Filter by order statuses (comma-separated)") 
            @RequestParam(required = false) List<Order.OrderStatus> statuses,
            
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 10)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field (default: createdAt)") @RequestParam(defaultValue = "createdAt") String sort) {
        
        log.info("Getting orders with filters - createdFrom: {}, createdTo: {}, statuses: {}", 
                createdFrom, createdTo, statuses);
        
        OrderFilterDto filterDto = new OrderFilterDto();
        filterDto.setCreatedFrom(createdFrom);
        filterDto.setCreatedTo(createdTo);
        filterDto.setStatuses(statuses);
        filterDto.setPage(page);
        filterDto.setSize(size);
        filterDto.setSort(sort);
        
        Page<OrderResponseDto> orders = orderServiceImpl.getOrdersWithFilters(filterDto);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get orders by user ID", description = "Retrieve paginated list of orders for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User orders retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OrderResponseDto>> getOrdersByUserId(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId,
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 10)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field (default: createdAt)") @RequestParam(defaultValue = "createdAt") String sort) {
        
        log.info("Getting orders for user: {}", userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<OrderResponseDto> orders = orderServiceImpl.getOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(
            summary = "Update order",
            description = "Update order status, user and/or items. " +
                    "Note: " +
                    "1. Order items can only be updated when order is in PENDING or CONFIRMED status. " +
                    "2. User can only be changed when order is in PENDING or CONFIRMED status. " +
                    "3. Status values: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Order, user or item not found"),
            @ApiResponse(responseCode = "409", description = "Cannot update in current order status")
    })
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.checkOrderAccess(#id, authentication)")
    @PutMapping("/{id}")
    public ResponseEntity<OrderResponseDto> updateOrder(
            @Parameter(description = "Order ID", required = true) @PathVariable Long id,
            @Valid @RequestBody OrderUpdateDto orderUpdateDto) {

        log.info("Updating order with id: {}", id);
        OrderResponseDto updatedOrder = orderServiceImpl.updateOrder(id, orderUpdateDto);
        return ResponseEntity.ok(updatedOrder);
    }

    @Operation(summary = "Delete order", description = "Soft delete an order by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.checkOrderAccess(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @Parameter(description = "Order ID", required = true) @PathVariable Long id) {
        
        log.info("Deleting order with id: {}", id);
        orderServiceImpl.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}