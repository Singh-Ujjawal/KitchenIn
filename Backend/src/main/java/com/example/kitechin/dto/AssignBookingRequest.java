package com.example.kitechin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignBookingRequest {

    @NotBlank(message = "Booking id is required")
    private String bookingId;

    @NotBlank(message = "Staff id is required")
    private String staffId;

    @NotNull(message = "Price is required")
    private Double price;
}
