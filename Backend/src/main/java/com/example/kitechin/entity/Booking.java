package com.example.kitechin.entity;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 1. ADD THESE IMPORTS
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@Document(collection = "bookings")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Booking {

    @Id
    // 2. ADD THIS TO ID
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    // 3. ADD THIS TO CUSTOMER ID
    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    @NotNull(message = "Customer ID is required")
    private ObjectId customerId;

    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId staffId;

    @NotNull(message = "Service category is required")
    private StaffCategory category;

    @Indexed
    private String city;

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    @NotBlank(message = "Time slot is required")
    private String timeSlot;

    @NotBlank(message = "Problem description is required")
    private String problemDescription;

    @NotNull(message = "Address is required")
    private String serviceAddress;

    private Double price;

    @Indexed
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @CreatedDate
    private LocalDateTime createdAt;
}