package com.stoliar.config;

import com.stoliar.util.JwtTokenProvider;
import com.stoliar.util.JwtTokenProviderTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider() {
            @Override
            public boolean validateToken(String token) { return true; }
            @Override
            public String getUsernameFromToken(String token) { return "test-user"; }
            @Override
            public Long getUserIdFromToken(String token) { return 1L; }
            @Override
            public String getRoleFromToken(String token) { return "USER"; }
        };
    }
}
