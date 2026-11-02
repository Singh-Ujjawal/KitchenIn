package com.example.kitechin.service;

import com.example.kitechin.common.Ids;
import com.example.kitechin.dto.ReviewRequest;
import com.example.kitechin.dto.ReviewResponse;
import com.example.kitechin.entity.Booking;
import com.example.kitechin.entity.BookingStatus;
import com.example.kitechin.entity.Customer;
import com.example.kitechin.entity.Review;
import com.example.kitechin.entity.Staff;
import com.example.kitechin.exception.ApiException;
import com.example.kitechin.repository.BookingRepository;
import com.example.kitechin.repository.CustomerRepository;
import com.example.kitechin.repository.ReviewRepository;
import com.example.kitechin.repository.StaffRepository;
import com.example.kitechin.security.AuthPrincipal;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         BookingRepository bookingRepository,
                         StaffRepository staffRepository,
                         CustomerRepository customerRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.staffRepository = staffRepository;
        this.customerRepository = customerRepository;
    }

    public ReviewResponse addReview(ReviewRequest request, AuthPrincipal principal) {
        ObjectId bookingId = Ids.parse(request.getBookingId(), "booking id");
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        ObjectId customerId = Ids.parse(principal.getId(), "customer id");
        if (!booking.getCustomerId().equals(customerId)) {
            throw ApiException.forbidden("You can only review your own bookings");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw ApiException.conflict("You can only review completed bookings");
        }
        if (booking.getStaffId() == null) {
            throw ApiException.conflict("This booking has no assigned staff to review");
        }
        if (reviewRepository.existsByBookingId(bookingId)) {
            throw ApiException.conflict("You have already reviewed this booking");
        }

        String comment = request.getComment() == null ? null : request.getComment().trim();
        Review review = Review.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .staffId(booking.getStaffId())
                .rating(request.getRating())
                .comment(comment == null || comment.isEmpty() ? null : comment)
                .createdAt(LocalDateTime.now())
                .build();
        Review saved = reviewRepository.save(review);
        updateStaffRating(booking.getStaffId());

        Customer customer = customerRepository.findById(customerId).orElse(null);
        return toResponse(saved, customer == null ? "Customer" : customer.getName());
    }

    public List<ReviewResponse> getReviewsForStaff(String staffId) {
        ObjectId id = Ids.parse(staffId, "staff id");
        if (!staffRepository.existsById(id)) {
            throw ApiException.notFound("Staff not found");
        }
        List<Review> reviews = reviewRepository.findByStaffId(id).stream()
                .sorted(Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Set<ObjectId> customerIds = new HashSet<>();
        reviews.forEach(review -> customerIds.add(review.getCustomerId()));
        Map<ObjectId, String> names = new HashMap<>();
        if (!customerIds.isEmpty()) {
            for (Customer customer : customerRepository.findAllById(customerIds)) {
                names.put(customer.getId(), customer.getName());
            }
        }
        return reviews.stream()
                .map(review -> toResponse(review, names.getOrDefault(review.getCustomerId(), "Customer")))
                .toList();
    }

    private void updateStaffRating(ObjectId staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> ApiException.notFound("Staff not found"));
        List<Review> reviews = reviewRepository.findByStaffId(staffId);
        if (reviews.isEmpty()) {
            staff.setRating(0.0);
            staff.setReviewCount(0);
        } else {
            double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
            staff.setRating(Math.round(average * 10.0) / 10.0);
            staff.setReviewCount(reviews.size());
        }
        staffRepository.save(staff);
    }

    private ReviewResponse toResponse(Review review, String customerName) {
        return ReviewResponse.builder()
                .id(review.getId() == null ? null : review.getId().toHexString())
                .bookingId(review.getBookingId() == null ? null : review.getBookingId().toHexString())
                .customerId(review.getCustomerId() == null ? null : review.getCustomerId().toHexString())
                .customerName(customerName)
                .staffId(review.getStaffId() == null ? null : review.getStaffId().toHexString())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
