package com.example.kitechin.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    public PasswordService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Accepts a BCrypt hash, or a legacy plain-text password.
     * A matching plain-text password is replaced with a BCrypt hash so the next login uses the hash.
     */
    public boolean verifyAndUpgrade(String rawPassword, String storedPassword, Consumer<String> upgrade) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (isBcrypt(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        if (storedPassword.equals(rawPassword)) {
            upgrade.accept(passwordEncoder.encode(rawPassword));
            return true;
        }
        return false;
    }

    private boolean isBcrypt(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
