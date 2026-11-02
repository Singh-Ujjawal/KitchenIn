package com.example.kitechin.controller;

import com.example.kitechin.dto.ReviewRequest;
import com.example.kitechin.dto.ReviewResponse;
import com.example.kitechin.security.CurrentUser;
import com.example.kitechin.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@Tag(name = "Review APIs")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ReviewResponse addReview(@Valid @RequestBody ReviewRequest request) {
        return reviewService.addReview(request, CurrentUser.require());
    }

    @GetMapping("/{staffId}")
    public List<ReviewResponse> getStaffReviews(@PathVariable String staffId) {
        return reviewService.getReviewsForStaff(staffId);
    }
}
