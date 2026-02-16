package com.stoliar.security;

import com.stoliar.entity.Order;
import com.stoliar.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderSecurity {
    
    private final OrderRepository orderRepository;
    
    public boolean checkOrderAccess(Long orderId, Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        
        // Проверяем, есть ли роль ADMIN
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (isAdmin) {
            return true;
        }
        
        // Получаем principal (userId)
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Long)) {
            return false;
        }
        
        Long userId = (Long) principal;
        
        // Проверяем, принадлежит ли заказ пользователю
        Optional<Order> order = orderRepository.findByIdAndNotDeleted(orderId);
        return order.isPresent() && order.get().getUserId().equals(userId);
    }
}