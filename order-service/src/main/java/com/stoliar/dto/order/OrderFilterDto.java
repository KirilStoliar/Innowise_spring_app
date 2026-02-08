package com.stoliar.dto.order;

import com.stoliar.entity.Order;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderFilterDto {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdFrom;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdTo;
    
    private List<Order.OrderStatus> statuses;

    @Min(value = 0, message = "Page number must be 0 or greater")
    private Integer page = 0;
    @Min(value = 1, message = "Page size must be at least 1")
    private Integer size = 10;

    private String sort = "createdAt";
}