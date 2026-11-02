package com.example.kitechin.service;

import com.example.kitechin.dto.StaffPublicResponse;
import com.example.kitechin.dto.UserResponse;
import com.example.kitechin.entity.Admin;
import com.example.kitechin.entity.Customer;
import com.example.kitechin.entity.Staff;

public final class UserMappings {

    private UserMappings() {
    }

    public static UserResponse fromCustomer(Customer customer) {
        return UserResponse.builder()
                .id(customer.getId().toHexString())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .role("CUSTOMER")
                .address(customer.getAddress())
                .build();
    }

    public static UserResponse fromStaff(Staff staff) {
        return UserResponse.builder()
                .id(staff.getId().toHexString())
                .name(staff.getName())
                .email(staff.getEmail())
                .phone(staff.getPhone())
                .role("WORKER")
                .city(staff.getCity())
                .category(staff.getCategory() == null ? null : staff.getCategory().name())
                .approved(staff.isApproved())
                .rating(staff.getRating())
                .reviewCount(staff.getReviewCount())
                .profilePicture(staff.getProfilePicture())
                .identityProof(staff.getIdentityProof())
                .build();
    }

    public static UserResponse fromAdmin(Admin admin) {
        return UserResponse.builder()
                .id(admin.getId().toHexString())
                .name(admin.getName())
                .email(admin.getEmail())
                .role("ADMIN")
                .build();
    }

    public static StaffPublicResponse toPublic(Staff staff) {
        return StaffPublicResponse.builder()
                .id(staff.getId().toHexString())
                .name(staff.getName())
                .category(staff.getCategory() == null ? null : staff.getCategory().name())
                .city(staff.getCity())
                .rating(staff.getRating())
                .reviewCount(staff.getReviewCount())
                .profilePicture(staff.getProfilePicture())
                .build();
    }
}
