package com.stoliar.util;

public class JwtTokenProviderTest extends JwtTokenProvider {
    @Override
    public boolean validateToken(String token) {
        return true;
    }

    @Override
    public String getUsernameFromToken(String token) {
        return "test-user";
    }

    @Override
    public Long getUserIdFromToken(String token) {
        return 1L;
    }

    @Override
    public String getRoleFromToken(String token) {
        return "USER";
    }
}