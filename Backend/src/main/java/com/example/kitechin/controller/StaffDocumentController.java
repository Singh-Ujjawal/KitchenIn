package com.example.kitechin.controller;

import com.example.kitechin.dto.UserResponse;
import com.example.kitechin.security.CurrentUser;
import com.example.kitechin.service.FileService;
import com.example.kitechin.service.StaffService;
import com.example.kitechin.service.StoredFile;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/staff/docs")
@Tag(name = "Staff Document APIs")
public class StaffDocumentController {

    private final StaffService staffService;
    private final FileService fileService;

    public StaffDocumentController(StaffService staffService, FileService fileService) {
        this.staffService = staffService;
        this.fileService = fileService;
    }

    @PostMapping(value = "/upload/{staffId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('WORKER','ADMIN')")
    public UserResponse uploadDocuments(@PathVariable String staffId,
                                        @RequestParam(value = "profilePic", required = false) MultipartFile profilePic,
                                        @RequestParam(value = "identityProof", required = false) MultipartFile identityProof) {
        return staffService.uploadDocuments(staffId, profilePic, identityProof, CurrentUser.require());
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<byte[]> getProfileImage(@PathVariable String filename) {
        StoredFile file = fileService.loadPublicImage(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(12)).cachePublic())
                .body(file.data());
    }

    @GetMapping("/identity/{filename}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> getIdentityProof(@PathVariable String filename) {
        StoredFile file = fileService.load(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.data());
    }
}
