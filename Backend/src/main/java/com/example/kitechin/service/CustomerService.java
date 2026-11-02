package com.example.kitechin.service;

import com.example.kitechin.common.Emails;
import com.example.kitechin.dto.AuthResponse;
import com.example.kitechin.dto.CustomerRegisterRequest;
import com.example.kitechin.dto.LoginRequest;
import com.example.kitechin.dto.UserResponse;
import com.example.kitechin.entity.Customer;
import com.example.kitechin.exception.ApiException;
import com.example.kitechin.repository.CustomerRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordService passwordService;
    private final AuthTokenService authTokenService;

    public CustomerService(CustomerRepository customerRepository,
                           PasswordService passwordService,
                           AuthTokenService authTokenService) {
        this.customerRepository = customerRepository;
        this.passwordService = passwordService;
        this.authTokenService = authTokenService;
    }

    public UserResponse register(CustomerRegisterRequest request) {
        String email = Emails.normalize(request.getEmail());
        if (customerRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw ApiException.conflict("An account with this email already exists");
        }

        Customer customer = Customer.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordService.encode(request.getPassword()))
                .phone(request.getPhone().trim())
                .address(request.getAddress().trim())
                .active(true)
                .build();
        return UserMappings.fromCustomer(customerRepository.save(customer));
    }

    public AuthResponse login(LoginRequest request) {
        String email = Emails.normalize(request.getEmail());
        Customer customer = customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        boolean matches = passwordService.verifyAndUpgrade(request.getPassword(), customer.getPassword(), hash -> {
            customer.setPassword(hash);
            customerRepository.save(customer);
        });
        if (!matches) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        return authTokenService.issue(UserMappings.fromCustomer(customer));
    }
}
