package com.example.kitechin.service;

import com.example.kitechin.dto.AuthResponse;
import com.example.kitechin.dto.LoginRequest;
import com.example.kitechin.dto.StaffRegisterRequest;
import com.example.kitechin.dto.UserResponse;
import com.example.kitechin.entity.Staff;
import com.example.kitechin.entity.StaffCategory;
import com.example.kitechin.exception.ApiException;
import com.example.kitechin.repository.BookingRepository;
import com.example.kitechin.repository.StaffRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private AuthTokenService authTokenService;
    @Mock
    private FileService fileService;

    private StaffService staffService;

    @BeforeEach
    void setUp() {
        staffService = new StaffService(
                staffRepository,
                bookingRepository,
                new PasswordService(new BCryptPasswordEncoder()),
                authTokenService,
                fileService
        );
    }

    @Test
    void registerStoresHashedPasswordAndLeavesStaffUnapproved() {
        when(staffRepository.findByEmailIgnoreCase("worker@example.com")).thenReturn(Optional.empty());
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> {
            Staff staff = invocation.getArgument(0);
            staff.setId(new ObjectId());
            return staff;
        });

        StaffRegisterRequest request = new StaffRegisterRequest();
        request.setName("Amit Worker");
        request.setEmail("worker@example.com");
        request.setPassword("secret1");
        request.setPhone("9876543210");
        request.setCity("lucknow");
        request.setCategory(StaffCategory.WORKER);

        UserResponse response = staffService.register(request);

        ArgumentCaptor<Staff> captor = ArgumentCaptor.forClass(Staff.class);
        verify(staffRepository).save(captor.capture());
        assertFalse(captor.getValue().isApproved());
        assertTrue(captor.getValue().getPassword().startsWith("$2"));
        assertEquals("Lucknow", captor.getValue().getCity());
        assertEquals(Boolean.FALSE, response.getApproved());
    }

    @Test
    void loginBeforeApprovalIsRejected() {
        Staff staff = approvedStaff(false, new BCryptPasswordEncoder().encode("secret1"));
        when(staffRepository.findByEmailIgnoreCase("worker@example.com")).thenReturn(Optional.of(staff));

        LoginRequest request = new LoginRequest();
        request.setEmail("worker@example.com");
        request.setPassword("secret1");

        ApiException exception = assertThrows(ApiException.class, () -> staffService.login(request));
        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void loginAfterApprovalIssuesToken() {
        Staff staff = approvedStaff(true, new BCryptPasswordEncoder().encode("secret1"));
        when(staffRepository.findByEmailIgnoreCase("worker@example.com")).thenReturn(Optional.of(staff));
        when(authTokenService.issue(any())).thenReturn(AuthResponse.builder().token("token").build());

        LoginRequest request = new LoginRequest();
        request.setEmail("worker@example.com");
        request.setPassword("secret1");

        AuthResponse response = staffService.login(request);
        assertEquals("token", response.getToken());
    }

    @Test
    void plainTextPasswordIsUpgradedOnLogin() {
        Staff staff = approvedStaff(true, "secret1");
        when(staffRepository.findByEmailIgnoreCase("worker@example.com")).thenReturn(Optional.of(staff));
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authTokenService.issue(any())).thenReturn(AuthResponse.builder().token("token").build());

        LoginRequest request = new LoginRequest();
        request.setEmail("worker@example.com");
        request.setPassword("secret1");

        staffService.login(request);
        assertTrue(staff.getPassword().startsWith("$2"));
    }

    @Test
    void removeIsBlockedWhenStaffHasBookings() {
        ObjectId id = new ObjectId();
        when(staffRepository.existsById(id)).thenReturn(true);
        when(bookingRepository.existsByStaffId(id)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> staffService.removeStaff(id.toHexString()));
        assertEquals(409, exception.getStatus().value());
        verify(staffRepository, never()).deleteById(any());
    }

    private Staff approvedStaff(boolean approved, String password) {
        Staff staff = Staff.builder()
                .name("Amit Worker")
                .email("worker@example.com")
                .password(password)
                .phone("9876543210")
                .city("Lucknow")
                .category(StaffCategory.WORKER)
                .approved(approved)
                .build();
        staff.setId(new ObjectId());
        return staff;
    }
}
