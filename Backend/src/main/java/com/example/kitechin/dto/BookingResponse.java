package com.example.kitechin.dto;

import com.example.kitechin.entity.BookingStatus;
import com.example.kitechin.entity.StaffCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class BookingResponse {
    private String id;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String staffId;
    private String staffName;
    private String staffPhone;
    private Double staffRating;
    private Integer staffReviewCount;
    private String staffProfilePicture;
    private StaffCategory category;
    private String city;
    private LocalDate bookingDate;
    private String timeSlot;
    private String problemDescription;
    private String serviceAddress;
    private Double price;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private boolean reviewed;
}
