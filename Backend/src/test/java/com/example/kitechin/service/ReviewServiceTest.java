package com.example.kitechin.service;

import com.example.kitechin.dto.ReviewRequest;
import com.example.kitechin.entity.Booking;
import com.example.kitechin.entity.BookingStatus;
import com.example.kitechin.entity.Customer;
import com.example.kitechin.entity.Review;
import com.example.kitechin.entity.Staff;
import com.example.kitechin.entity.StaffCategory;
import com.example.kitechin.exception.ApiException;
import com.example.kitechin.repository.BookingRepository;
import com.example.kitechin.repository.CustomerRepository;
import com.example.kitechin.repository.ReviewRepository;
import com.example.kitechin.repository.StaffRepository;
import com.example.kitechin.security.AuthPrincipal;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private CustomerRepository customerRepository;

    private ReviewService reviewService;
    private ObjectId customerId;
    private ObjectId staffId;
    private ObjectId bookingId;
    private AuthPrincipal customer;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, bookingRepository, staffRepository, customerRepository);
        customerId = new ObjectId();
        staffId = new ObjectId();
        bookingId = new ObjectId();
        customer = new AuthPrincipal(customerId.toHexString(), "rahul@example.com", "Rahul", "CUSTOMER");
    }

    @Test
    void duplicateReviewIsRejected() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(completedBooking()));
        when(reviewRepository.existsByBookingId(bookingId)).thenReturn(true);

        ReviewRequest request = new ReviewRequest();
        request.setBookingId(bookingId.toHexString());
        request.setRating(5);

        assertEquals(409, assertThrows(ApiException.class, () -> reviewService.addReview(request, customer)).getStatus().value());
    }

    @Test
    void reviewUpdatesAverageRating() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(completedBooking()));
        when(reviewRepository.existsByBookingId(bookingId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(new ObjectId());
            return review;
        });
        Staff staff = Staff.builder()
                .name("Sunita")
                .email("sunita@example.com")
                .password("hashed")
                .phone("9876543211")
                .city("Lucknow")
                .category(StaffCategory.MAID)
                .approved(true)
                .rating(0.0)
                .reviewCount(0)
                .build();
        staff.setId(staffId);
        when(staffRepository.findById(staffId)).thenReturn(Optional.of(staff));
        Review existing = Review.builder().rating(4).staffId(staffId).customerId(customerId).bookingId(new ObjectId()).build();
        Review created = Review.builder().rating(5).staffId(staffId).customerId(customerId).bookingId(bookingId).build();
        when(reviewRepository.findByStaffId(staffId)).thenReturn(List.of(existing, created));
        Customer customerEntity = Customer.builder()
                .name("Rahul")
                .email("rahul@example.com")
                .password("hashed")
                .phone("9876543210")
                .address("12 MG Road")
                .build();
        customerEntity.setId(customerId);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerEntity));

        ReviewRequest request = new ReviewRequest();
        request.setBookingId(bookingId.toHexString());
        request.setRating(5);
        request.setComment("Careful and on time");

        reviewService.addReview(request, customer);

        ArgumentCaptor<Staff> captor = ArgumentCaptor.forClass(Staff.class);
        verify(staffRepository).save(captor.capture());
        assertEquals(4.5, captor.getValue().getRating());
        assertEquals(2, captor.getValue().getReviewCount());
    }

    @Test
    void pendingBookingCannotBeReviewed() {
        Booking booking = completedBooking();
        booking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        ReviewRequest request = new ReviewRequest();
        request.setBookingId(bookingId.toHexString());
        request.setRating(5);

        assertEquals("You can only review completed bookings",
                assertThrows(ApiException.class, () -> reviewService.addReview(request, customer)).getMessage());
    }

    private Booking completedBooking() {
        Booking booking = Booking.builder()
                .customerId(customerId)
                .staffId(staffId)
                .category(StaffCategory.MAID)
                .city("Lucknow")
                .status(BookingStatus.COMPLETED)
                .problemDescription("Cleaning")
                .serviceAddress("12 MG Road")
                .timeSlot("Morning (9AM - 12PM)")
                .build();
        booking.setId(bookingId);
        return booking;
    }
}
