package com.example.kitechin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewResponse {
    private String id;
    private String bookingId;
    private String customerId;
    private String customerName;
    private String staffId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
