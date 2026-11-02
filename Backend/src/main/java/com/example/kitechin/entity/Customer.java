package com.example.kitechin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.*;

// IMPORT THESE 2 LINES
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@Document(collection = "customers")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Customer {

    @Id
    // THIS FIXES THE ID ISSUE
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email")
    @Indexed(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @JsonIgnore
    @ToString.Exclude
    private String password;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    private String address;

    @Builder.Default
    private boolean active = true;
}