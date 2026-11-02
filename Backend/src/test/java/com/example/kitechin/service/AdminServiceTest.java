package com.example.kitechin.service;

import com.example.kitechin.dto.AdminRegisterRequest;
import com.example.kitechin.dto.LoginRequest;
import com.example.kitechin.entity.Admin;
import com.example.kitechin.exception.ApiException;
import com.example.kitechin.repository.AdminRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;
    @Mock
    private AuthTokenService authTokenService;

    private AdminService adminService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        adminService = new AdminService(adminRepository, new PasswordService(encoder), authTokenService);
    }

    @Test
    void firstAdminCanRegisterAndPasswordIsHashed() {
        when(adminRepository.count()).thenReturn(0L);
        when(adminRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> {
            Admin admin = invocation.getArgument(0);
            admin.setId(new ObjectId());
            return admin;
        });

        AdminRegisterRequest request = new AdminRegisterRequest();
        request.setName("Platform Admin");
        request.setEmail("admin@example.com");
        request.setPassword("secret1");

        adminService.register(request);

        ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
        org.mockito.Mockito.verify(adminRepository).save(captor.capture());
        assertTrue(captor.getValue().getPassword().startsWith("$2"));
    }

    @Test
    void laterAdminRegistrationIsRejectedWithoutAnAdminSession() {
        when(adminRepository.count()).thenReturn(1L);
        AdminRegisterRequest request = new AdminRegisterRequest();
        request.setName("Another Admin");
        request.setEmail("other@example.com");
        request.setPassword("secret1");

        assertEquals(403, assertThrows(ApiException.class, () -> adminService.register(request)).getStatus().value());
    }

    @Test
    void loginRejectsAWrongPassword() {
        Admin admin = Admin.builder().name("Platform Admin").email("admin@example.com").password(encoder.encode("secret1")).build();
        admin.setId(new ObjectId());
        when(adminRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        LoginRequest request = new LoginRequest();
        request.setEmail("admin@example.com");
        request.setPassword("wrong-password");

        assertEquals(401, assertThrows(ApiException.class, () -> adminService.login(request)).getStatus().value());
    }
}
