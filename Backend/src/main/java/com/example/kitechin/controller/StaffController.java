package com.example.kitechin.controller;

import com.example.kitechin.dto.AuthResponse;
import com.example.kitechin.dto.LoginRequest;
import com.example.kitechin.dto.StaffPublicResponse;
import com.example.kitechin.dto.StaffRegisterRequest;
import com.example.kitechin.dto.UserResponse;
import com.example.kitechin.entity.StaffCategory;
import com.example.kitechin.service.StaffService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/staff")
@Tag(name = "Staff APIs")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody StaffRegisterRequest request) {
        return staffService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return staffService.login(request);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getPendingApprovals() {
        return staffService.getPendingApprovals();
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse approve(@PathVariable String id) {
        return staffService.approveStaff(id);
    }

    @DeleteMapping("/remove/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStaff(@PathVariable String id) {
        staffService.removeStaff(id);
    }

    @GetMapping("/category/{category}")
    public List<StaffPublicResponse> getByCategory(@PathVariable StaffCategory category) {
        return staffService.getByCategory(category);
    }

    @GetMapping("/search/{city}/{category}")
    public List<StaffPublicResponse> searchStaff(@PathVariable String city,
                                                 @PathVariable StaffCategory category) {
        return staffService.search(city, category);
    }

    @GetMapping("/top")
    public List<StaffPublicResponse> topRated() {
        return staffService.topRated();
    }
}
