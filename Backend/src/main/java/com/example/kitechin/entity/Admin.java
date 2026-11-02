package com.example.kitechin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.*;

@Document(collection = "admins")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Admin {

    @Id
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
}
