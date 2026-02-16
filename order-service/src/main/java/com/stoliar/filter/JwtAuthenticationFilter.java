package com.stoliar.filter;

import com.stoliar.util.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);
                Long userId = jwtTokenProvider.getUserIdFromToken(token);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

                // Устанавливаем userId как principal
                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user: {} with role: {} and userId: {}", username, role, userId);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/api/v1/orders/internal/");
    }
}