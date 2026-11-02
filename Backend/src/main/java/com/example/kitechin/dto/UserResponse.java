package com.example.kitechin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String address;
    private String city;
    private String category;
    private Boolean approved;
    private Double rating;
    private Integer reviewCount;
    private String profilePicture;
    private String identityProof;
}
