package com.example.kitechin.security;

import com.example.kitechin.entity.Admin;
import com.example.kitechin.entity.Customer;
import com.example.kitechin.entity.Staff;
import com.example.kitechin.exception.ApiError;
import com.example.kitechin.exception.ApiException;
import com.example.kitechin.repository.AdminRepository;
import com.example.kitechin.repository.CustomerRepository;
import com.example.kitechin.repository.StaffRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.types.ObjectId;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final AdminRepository adminRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomerRepository customerRepository,
                                   StaffRepository staffRepository,
                                   AdminRepository adminRepository,
                                   ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
        this.adminRepository = adminRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        try {
            var claims = jwtService.parse(token);
            String role = claims.get("role", String.class);
            String id = claims.getSubject();
            if (role == null || id == null || !ObjectId.isValid(id)) {
                throw ApiException.unauthorized("Invalid authentication token");
            }

            AuthPrincipal principal = loadPrincipal(role, new ObjectId(id));
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (ApiException exception) {
            SecurityContextHolder.clearContext();
            if (isLoginOrRegister(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            writeError(response, exception.getStatus().value(), exception.getMessage());
        }
    }

    private AuthPrincipal loadPrincipal(String role, ObjectId id) {
        return switch (role) {
            case "CUSTOMER" -> {
                Customer customer = customerRepository.findById(id)
                        .orElseThrow(() -> ApiException.unauthorized("Account not found. Please sign in again."));
                yield new AuthPrincipal(customer.getId().toHexString(), customer.getEmail(), customer.getName(), "CUSTOMER");
            }
            case "WORKER" -> {
                Staff staff = staffRepository.findById(id)
                        .orElseThrow(() -> ApiException.unauthorized("Account not found. Please sign in again."));
                if (!staff.isApproved()) {
                    throw ApiException.forbidden("Your account is pending admin approval");
                }
                yield new AuthPrincipal(staff.getId().toHexString(), staff.getEmail(), staff.getName(), "WORKER");
            }
            case "ADMIN" -> {
                Admin admin = adminRepository.findById(id)
                        .orElseThrow(() -> ApiException.unauthorized("Account not found. Please sign in again."));
                yield new AuthPrincipal(admin.getId().toHexString(), admin.getEmail(), admin.getName(), "ADMIN");
            }
            default -> throw ApiException.unauthorized("Invalid authentication token");
        };
    }

    private boolean isLoginOrRegister(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.endsWith("/login") || path.endsWith("/register");
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiError body = ApiError.builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
