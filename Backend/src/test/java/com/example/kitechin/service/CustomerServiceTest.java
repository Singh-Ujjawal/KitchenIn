package com.example.kitechin.service;

import com.example.kitechin.dto.CustomerRegisterRequest;
import com.example.kitechin.dto.LoginRequest;
import com.example.kitechin.entity.Customer;
import com.example.kitechin.exception.ApiException;
import com.example.kitechin.repository.CustomerRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AuthTokenService authTokenService;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(
                customerRepository,
                new PasswordService(new BCryptPasswordEncoder()),
                authTokenService
        );
    }

    @Test
    void registerHashesPasswordAndRejectsDuplicateEmail() {
        when(customerRepository.findByEmailIgnoreCase("rahul@example.com")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(new ObjectId());
            return customer;
        });

        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setName("Rahul Sharma");
        request.setEmail("Rahul@Example.com");
        request.setPassword("secret1");
        request.setPhone("9876543210");
        request.setAddress("12 MG Road");

        customerService.register(request);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertEquals("rahul@example.com", captor.getValue().getEmail());
        assertTrue(captor.getValue().getPassword().startsWith("$2"));

        when(customerRepository.findByEmailIgnoreCase("rahul@example.com"))
                .thenReturn(Optional.of(captor.getValue()));
        assertEquals(409, assertThrows(ApiException.class, () -> customerService.register(request)).getStatus().value());
    }

    @Test
    void loginRejectsUnknownCustomer() {
        when(customerRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("secret1");

        assertEquals(401, assertThrows(ApiException.class, () -> customerService.login(request)).getStatus().value());
    }
}
