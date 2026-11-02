package com.example.kitechin.controller;

import com.example.kitechin.dto.AssignBookingRequest;
import com.example.kitechin.dto.BookingRequest;
import com.example.kitechin.dto.BookingResponse;
import com.example.kitechin.security.CurrentUser;
import com.example.kitechin.service.BookingService;
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
@RequestMapping("/bookings")
@Tag(name = "Booking APIs")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/book")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public BookingResponse createBooking(@Valid @RequestBody BookingRequest request) {
        return bookingService.createBooking(request, CurrentUser.require());
    }

    @GetMapping("/history/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public List<BookingResponse> getCustomerHistory(@PathVariable String customerId) {
        return bookingService.getCustomerBookings(customerId, CurrentUser.require());
    }

    @PostMapping("/cancel/{bookingId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public BookingResponse cancelBooking(@PathVariable String bookingId) {
        return bookingService.cancelBooking(bookingId, CurrentUser.require());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingResponse> getPendingBookings() {
        return bookingService.getPendingBookings();
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public BookingResponse assignStaff(@Valid @RequestBody AssignBookingRequest request) {
        return bookingService.assignStaff(request);
    }

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasAnyRole('WORKER','ADMIN')")
    public List<BookingResponse> getStaffJobs(@PathVariable String staffId) {
        return bookingService.getStaffBookings(staffId, CurrentUser.require());
    }

    @PostMapping("/complete/{bookingId}")
    @PreAuthorize("hasRole('WORKER')")
    public BookingResponse completeJob(@PathVariable String bookingId) {
        return bookingService.completeBooking(bookingId, CurrentUser.require());
    }
}
