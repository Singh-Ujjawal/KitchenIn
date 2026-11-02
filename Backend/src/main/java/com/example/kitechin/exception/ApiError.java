package com.example.kitechin.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiError {
    private final boolean success;
    private final String message;
    private final LocalDateTime timestamp;
}
