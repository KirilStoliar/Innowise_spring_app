package com.stoliar.security;

import com.stoliar.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSecurity {
    
    private final PaymentRepository paymentRepository;
    
    public Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            throw new SecurityException("User not authenticated");
        }
        return (Long) authentication.getPrincipal();
    }

    public boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
    
    public boolean checkPaymentAccess(String paymentId, Authentication authentication) {
        if (authentication == null) {
            log.warn("Authentication is null");
            return false;
        }
        
        // Проверяем, есть ли роль ADMIN
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (isAdmin) {
            log.debug("Admin access granted for payment: {}", paymentId);
            return true;
        }
        
        // Получаем principal (userId)
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Long)) {
            log.warn("Principal is not Long type: {}", principal.getClass().getName());
            return false;
        }
        
        Long userId = (Long) principal;
        
        // Проверяем, принадлежит ли платеж пользователю
        boolean hasAccess = paymentRepository.findById(paymentId)
                .map(payment -> payment.getUserId().equals(userId))
                .orElse(false);
        
        log.debug("User {} access to payment {}: {}", userId, paymentId, hasAccess);
        return hasAccess;
    }
    
    public boolean checkUserAccess(Long targetUserId, Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        
        // ADMIN может получать доступ к любым пользователям
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (isAdmin) {
            return true;
        }
        
        // Пользователь может получать доступ только к своим данным
        Object principal = authentication.getPrincipal();
        return principal instanceof Long && ((Long) principal).equals(targetUserId);
    }
}