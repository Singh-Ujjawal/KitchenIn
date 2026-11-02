package com.example.kitechin.service;

import com.example.kitechin.dto.AuthResponse;
import com.example.kitechin.dto.UserResponse;
import com.example.kitechin.security.JwtService;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {

    private final JwtService jwtService;

    public AuthTokenService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public AuthResponse issue(UserResponse user) {
        String token = jwtService.generateToken(user.getId(), user.getRole(), user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .user(user)
                .build();
    }
}
