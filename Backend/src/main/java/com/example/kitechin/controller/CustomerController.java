package com.example.kitechin.controller;

import com.example.kitechin.dto.AuthResponse;
import com.example.kitechin.dto.CustomerRegisterRequest;
import com.example.kitechin.dto.LoginRequest;
import com.example.kitechin.dto.UserResponse;
import com.example.kitechin.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
@Tag(name = "Customer APIs")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody CustomerRegisterRequest request) {
        return customerService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return customerService.login(request);
    }
}
