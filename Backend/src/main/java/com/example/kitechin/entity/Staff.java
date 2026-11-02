package com.example.kitechin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.*;

// ADD THESE IMPORTS
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@Document(collection = "staffs")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Staff {

    @Id
    // THIS LINE FIXES THE "[object Object]" ERROR
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name should be between 2 and 50 characters")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Indexed(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    @JsonIgnore
    @ToString.Exclude
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phone;

    @NotBlank(message = "City is required")
    @Indexed
    private String city;

    @NotNull(message = "Category is required")
    private StaffCategory category;

    @Builder.Default
    private boolean approved = false;

    @Builder.Default
    private Double rating = 0.0;

    @Builder.Default
    private Integer reviewCount = 0;

    private String profilePicture;
    private String identityProof;
}