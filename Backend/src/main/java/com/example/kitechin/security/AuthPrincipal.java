package com.example.kitechin.security;

import lombok.Getter;

@Getter
public class AuthPrincipal {

    private final String id;
    private final String email;
    private final String name;
    private final String role;

    public AuthPrincipal(String id, String email, String name, String role) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
    }
}
