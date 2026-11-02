package com.example.kitechin.service;

import com.example.kitechin.common.Emails;
import com.example.kitechin.dto.AdminRegisterRequest;
import com.example.kitechin.dto.AuthResponse;
import com.example.kitechin.dto.LoginRequest;
import com.example.kitechin.dto.UserResponse;
import com.example.kitechin.entity.Admin;
import com.example.kitechin.exception.ApiException;
import com.example.kitechin.repository.AdminRepository;
import com.example.kitechin.security.AuthPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordService passwordService;
    private final AuthTokenService authTokenService;
    private final String signupCode;

    @Autowired
    public AdminService(AdminRepository adminRepository,
                        PasswordService passwordService,
                        AuthTokenService authTokenService,
                        @Value("${app.admin.signup-code:}") String signupCode) {
        this.adminRepository = adminRepository;
        this.passwordService = passwordService;
        this.authTokenService = authTokenService;
        this.signupCode = signupCode == null ? "" : signupCode.trim();
    }

    public AdminService(AdminRepository adminRepository,
                        PasswordService passwordService,
                        AuthTokenService authTokenService) {
        this(adminRepository, passwordService, authTokenService, "");
    }

    public UserResponse register(AdminRegisterRequest request) {
        if (adminRepository.count() > 0 && !callerIsAdmin() && !validSignupCode(request.getAdminCode())) {
            throw ApiException.forbidden("Admin registration is restricted");
        }
        String email = Emails.normalize(request.getEmail());
        if (adminRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw ApiException.conflict("An account with this email already exists");
        }

        Admin admin = Admin.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordService.encode(request.getPassword()))
                .build();
        return UserMappings.fromAdmin(adminRepository.save(admin));
    }

    public AuthResponse login(LoginRequest request) {
        String email = Emails.normalize(request.getEmail());
        Admin admin = adminRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        boolean matches = passwordService.verifyAndUpgrade(request.getPassword(), admin.getPassword(), hash -> {
            admin.setPassword(hash);
            adminRepository.save(admin);
        });
        if (!matches) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        return authTokenService.issue(UserMappings.fromAdmin(admin));
    }

    private boolean validSignupCode(String provided) {
        return !signupCode.isEmpty() && provided != null
                && java.security.MessageDigest.isEqual(
                        signupCode.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        provided.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private boolean callerIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal
                && "ADMIN".equals(principal.getRole());
    }
}
