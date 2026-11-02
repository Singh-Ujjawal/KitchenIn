package com.example.kitechin.service;

import com.example.kitechin.common.Emails;
import com.example.kitechin.common.Ids;
import com.example.kitechin.common.ServiceCities;
import com.example.kitechin.dto.AuthResponse;
import com.example.kitechin.dto.LoginRequest;
import com.example.kitechin.dto.StaffPublicResponse;
import com.example.kitechin.dto.StaffRegisterRequest;
import com.example.kitechin.dto.UserResponse;
import com.example.kitechin.entity.Staff;
import com.example.kitechin.entity.StaffCategory;
import com.example.kitechin.exception.ApiException;
import com.example.kitechin.repository.BookingRepository;
import com.example.kitechin.repository.StaffRepository;
import com.example.kitechin.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class StaffService {

    private final StaffRepository staffRepository;
    private final BookingRepository bookingRepository;
    private final PasswordService passwordService;
    private final AuthTokenService authTokenService;
    private final FileService fileService;

    public StaffService(StaffRepository staffRepository,
                        BookingRepository bookingRepository,
                        PasswordService passwordService,
                        AuthTokenService authTokenService,
                        FileService fileService) {
        this.staffRepository = staffRepository;
        this.bookingRepository = bookingRepository;
        this.passwordService = passwordService;
        this.authTokenService = authTokenService;
        this.fileService = fileService;
    }

    public UserResponse register(StaffRegisterRequest request) {
        String email = Emails.normalize(request.getEmail());
        if (staffRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw ApiException.conflict("An account with this email already exists");
        }

        Staff staff = Staff.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordService.encode(request.getPassword()))
                .phone(request.getPhone().trim())
                .city(ServiceCities.canonical(request.getCity()))
                .category(request.getCategory())
                .approved(false)
                .rating(0.0)
                .reviewCount(0)
                .build();
        return UserMappings.fromStaff(staffRepository.save(staff));
    }

    public AuthResponse login(LoginRequest request) {
        String email = Emails.normalize(request.getEmail());
        Staff staff = staffRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        boolean matches = passwordService.verifyAndUpgrade(request.getPassword(), staff.getPassword(), hash -> {
            staff.setPassword(hash);
            staffRepository.save(staff);
        });
        if (!matches) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        if (!staff.isApproved()) {
            throw ApiException.forbidden("Your account is pending admin approval");
        }
        return authTokenService.issue(UserMappings.fromStaff(staff));
    }

    public List<UserResponse> getPendingApprovals() {
        return staffRepository.findByApproved(false).stream()
                .map(UserMappings::fromStaff)
                .toList();
    }

    public UserResponse approveStaff(String id) {
        Staff staff = staffRepository.findById(Ids.parse(id, "staff id"))
                .orElseThrow(() -> ApiException.notFound("Staff not found"));
        staff.setApproved(true);
        return UserMappings.fromStaff(staffRepository.save(staff));
    }

    public void removeStaff(String id) {
        var objectId = Ids.parse(id, "staff id");
        if (!staffRepository.existsById(objectId)) {
            throw ApiException.notFound("Staff not found");
        }
        if (bookingRepository.existsByStaffId(objectId)) {
            throw ApiException.conflict("This staff member has bookings and cannot be removed");
        }
        staffRepository.deleteById(objectId);
    }

    public List<StaffPublicResponse> getByCategory(StaffCategory category) {
        return staffRepository.findByCategoryAndApproved(category, true).stream()
                .map(UserMappings::toPublic)
                .toList();
    }

    public List<StaffPublicResponse> search(String city, StaffCategory category) {
        String canonicalCity = ServiceCities.canonical(city);
        return staffRepository.findByCityIgnoreCaseAndCategoryAndApproved(canonicalCity, category, true).stream()
                .map(UserMappings::toPublic)
                .toList();
    }

    public List<StaffPublicResponse> topRated() {
        return staffRepository.findTop6ByApprovedTrueOrderByRatingDesc().stream()
                .map(UserMappings::toPublic)
                .toList();
    }

    public UserResponse uploadDocuments(String staffId,
                                        MultipartFile profilePic,
                                        MultipartFile identityProof,
                                        AuthPrincipal principal) {
        if ("WORKER".equals(principal.getRole()) && !principal.getId().equals(staffId)) {
            throw ApiException.forbidden("You can only upload your own documents");
        }
        boolean hasProfile = profilePic != null && !profilePic.isEmpty();
        boolean hasIdentity = identityProof != null && !identityProof.isEmpty();
        if (!hasProfile && !hasIdentity) {
            throw ApiException.badRequest("Choose a profile picture or an identity document");
        }

        Staff staff = staffRepository.findById(Ids.parse(staffId, "staff id"))
                .orElseThrow(() -> ApiException.notFound("Staff not found"));
        if (hasProfile) {
            staff.setProfilePicture(fileService.storeImage(profilePic));
        }
        if (hasIdentity) {
            staff.setIdentityProof(fileService.storeIdentity(identityProof));
        }
        return UserMappings.fromStaff(staffRepository.save(staff));
    }
}
