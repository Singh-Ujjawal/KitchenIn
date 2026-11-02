package com.example.kitechin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffPublicResponse {
    private String id;
    private String name;
    private String category;
    private String city;
    private Double rating;
    private Integer reviewCount;
    private String profilePicture;
}
