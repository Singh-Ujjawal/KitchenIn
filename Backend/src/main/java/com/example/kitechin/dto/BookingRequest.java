package com.example.kitechin.dto;

import com.example.kitechin.entity.StaffCategory;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BookingRequest {

    @NotNull(message = "Service category is required")
    private StaffCategory category;

    @NotNull(message = "Booking date is required")
    @FutureOrPresent(message = "Booking date cannot be in the past")
    private LocalDate bookingDate;

    @NotBlank(message = "Time slot is required")
    private String timeSlot;

    @NotBlank(message = "Problem description is required")
    @Size(max = 1000, message = "Problem description cannot exceed 1000 characters")
    private String problemDescription;

    @NotBlank(message = "Address is required")
    @Size(max = 300, message = "Address cannot exceed 300 characters")
    private String serviceAddress;

    @NotBlank(message = "City is required")
    private String city;
}
