package com.example.kitechin.service;

import com.example.kitechin.dto.AssignBookingRequest;
import com.example.kitechin.dto.BookingRequest;
import com.example.kitechin.dto.BookingResponse;
import com.example.kitechin.entity.Booking;
import com.example.kitechin.entity.BookingStatus;
import com.example.kitechin.entity.Customer;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private MongoTemplate mongoTemplate;

    private BookingService bookingService;
    private ObjectId customerId;
    private AuthPrincipal customer;
    private AuthPrincipal otherCustomer;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository, customerRepository, staffRepository, reviewRepository, mongoTemplate);
        customerId = new ObjectId();
        customer = new AuthPrincipal(customerId.toHexString(), "rahul@example.com", "Rahul", "CUSTOMER");
        otherCustomer = new AuthPrincipal(new ObjectId().toHexString(), "other@example.com", "Other", "CUSTOMER");
    }

    @Test
    void newBookingStartsPendingWithoutStaffOrPrice() {
        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(new ObjectId());
            return booking;
        });
        when(customerRepository.findAllById(any())).thenReturn(List.of(customerEntity()));
        when(reviewRepository.findByBookingIdIn(any())).thenReturn(List.of());

        BookingResponse response = bookingService.createBooking(request(), customer);

        assertEquals(BookingStatus.PENDING, response.getStatus());
        assertEquals("Lucknow", response.getCity());
        assertEquals(null, response.getStaffId());
        assertEquals(null, response.getPrice());
    }

    @Test
    void assignmentRejectsUnapprovedStaff() {
        Booking booking = pendingBooking();
        Staff staff = staff(false, StaffCategory.MAID, "Lucknow");
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));

        ApiException exception = assertThrows(ApiException.class,
                () -> bookingService.assignStaff(assign(booking, staff, 500.0)));
        assertEquals(409, exception.getStatus().value());
        verify(mongoTemplate, never()).findAndModify(any(), any(), any(), eq(Booking.class));
    }

    @Test
    void assignmentRejectsCategoryMismatch() {
        Booking booking = pendingBooking();
        Staff staff = staff(true, StaffCategory.MECHANIC, "Lucknow");
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));

        ApiException exception = assertThrows(ApiException.class,
                () -> bookingService.assignStaff(assign(booking, staff, 500.0)));
        assertEquals("Staff category does not match the booking", exception.getMessage());
    }

    @Test
    void assignmentRejectsCityMismatch() {
        Booking booking = pendingBooking();
        Staff staff = staff(true, StaffCategory.MAID, "Patna");
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));

        ApiException exception = assertThrows(ApiException.class,
                () -> bookingService.assignStaff(assign(booking, staff, 500.0)));
        assertEquals("Staff city does not match the booking city", exception.getMessage());
    }

    @Test
    void assignmentRejectsNonPositivePrice() {
        AssignBookingRequest request = new AssignBookingRequest();
        request.setBookingId(new ObjectId().toHexString());
        request.setStaffId(new ObjectId().toHexString());
        request.setPrice(0.0);

        assertEquals(400, assertThrows(ApiException.class, () -> bookingService.assignStaff(request)).getStatus().value());
    }

    @Test
    void assignmentRejectsBookingThatIsNotPending() {
        Booking booking = pendingBooking();
        booking.setStatus(BookingStatus.ASSIGNED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ApiException exception = assertThrows(ApiException.class,
                () -> bookingService.assignStaff(assign(booking, staff(true, StaffCategory.MAID, "Lucknow"), 800.0)));
        assertEquals("This booking is already assigned", exception.getMessage());
    }

    @Test
    void customerCannotCancelSomeoneElsesBooking() {
        Booking booking = pendingBooking();
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ApiException exception = assertThrows(ApiException.class,
                () -> bookingService.cancelBooking(booking.getId().toHexString(), otherCustomer));
        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void completedBookingCannotBeCancelled() {
        Booking booking = pendingBooking();
        booking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ApiException exception = assertThrows(ApiException.class,
                () -> bookingService.cancelBooking(booking.getId().toHexString(), customer));
        assertEquals("A completed booking cannot be cancelled", exception.getMessage());
    }

    @Test
    void customerCanCancelOwnPendingBooking() {
        Booking booking = pendingBooking();
        Booking cancelled = copy(booking);
        cancelled.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Booking.class)))
                .thenReturn(cancelled);
        when(customerRepository.findAllById(any())).thenReturn(List.of(customerEntity()));
        when(reviewRepository.findByBookingIdIn(any())).thenReturn(List.of());

        BookingResponse response = bookingService.cancelBooking(booking.getId().toHexString(), customer);
        assertEquals(BookingStatus.CANCELLED, response.getStatus());
    }

    @Test
    void wrongStaffCannotCompleteBooking() {
        Booking booking = pendingBooking();
        booking.setStatus(BookingStatus.ASSIGNED);
        booking.setStaffId(new ObjectId());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        AuthPrincipal staffUser = new AuthPrincipal(new ObjectId().toHexString(), "s@example.com", "Sunita", "WORKER");

        ApiException exception = assertThrows(ApiException.class,
                () -> bookingService.completeBooking(booking.getId().toHexString(), staffUser));
        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void cancelledBookingCannotBeCompleted() {
        Booking booking = pendingBooking();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setStaffId(new ObjectId());
        AuthPrincipal staffUser = new AuthPrincipal(booking.getStaffId().toHexString(), "s@example.com", "Sunita", "WORKER");
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        ApiException exception = assertThrows(ApiException.class,
                () -> bookingService.completeBooking(booking.getId().toHexString(), staffUser));
        assertEquals("A cancelled booking cannot be completed", exception.getMessage());
    }

    @Test
    void assignedStaffCanCompleteBooking() {
        ObjectId staffId = new ObjectId();
        Booking booking = pendingBooking();
        booking.setStatus(BookingStatus.ASSIGNED);
        booking.setStaffId(staffId);
        Booking completed = copy(booking);
        completed.setStatus(BookingStatus.COMPLETED);
        AuthPrincipal staffUser = new AuthPrincipal(staffId.toHexString(), "s@example.com", "Sunita", "WORKER");

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Booking.class)))
                .thenReturn(completed);
        when(customerRepository.findAllById(any())).thenReturn(List.of(customerEntity()));
        when(staffRepository.findAllById(any())).thenReturn(List.of(staff(true, StaffCategory.MAID, "Lucknow")));
        when(reviewRepository.findByBookingIdIn(any())).thenReturn(List.of());

        BookingResponse response = bookingService.completeBooking(booking.getId().toHexString(), staffUser);
        assertEquals(BookingStatus.COMPLETED, response.getStatus());
    }

    private BookingRequest request() {
        BookingRequest request = new BookingRequest();
        request.setCategory(StaffCategory.MAID);
        request.setBookingDate(LocalDate.now());
        request.setTimeSlot("Morning (9AM - 12PM)");
        request.setProblemDescription("Need a full house cleaning");
        request.setServiceAddress("12 MG Road");
        request.setCity("Lucknow");
        return request;
    }

    private Customer customerEntity() {
        Customer customer = Customer.builder()
                .name("Rahul")
                .email("rahul@example.com")
                .password("hashed")
                .phone("9876543210")
                .address("12 MG Road")
                .build();
        customer.setId(customerId);
        return customer;
    }

    private Booking pendingBooking() {
        Booking booking = Booking.builder()
                .customerId(customerId)
                .category(StaffCategory.MAID)
                .city("Lucknow")
                .bookingDate(LocalDate.now().plusDays(1))
                .timeSlot("Morning (9AM - 12PM)")
                .problemDescription("Cleaning")
                .serviceAddress("12 MG Road")
                .status(BookingStatus.PENDING)
                .build();
        booking.setId(new ObjectId());
        return booking;
    }

    private Booking copy(Booking booking) {
        Booking copy = Booking.builder()
                .customerId(booking.getCustomerId())
                .staffId(booking.getStaffId())
                .category(booking.getCategory())
                .city(booking.getCity())
                .bookingDate(booking.getBookingDate())
                .timeSlot(booking.getTimeSlot())
                .problemDescription(booking.getProblemDescription())
                .serviceAddress(booking.getServiceAddress())
                .price(booking.getPrice())
                .status(booking.getStatus())
                .build();
        copy.setId(booking.getId());
        return copy;
    }

    private Staff staff(boolean approved, StaffCategory category, String city) {
        Staff staff = Staff.builder()
                .name("Sunita")
                .email("sunita@example.com")
                .password("hashed")
                .phone("9876543211")
                .city(city)
                .category(category)
                .approved(approved)
                .build();
        staff.setId(new ObjectId());
        return staff;
    }

    private AssignBookingRequest assign(Booking booking, Staff staff, double price) {
        AssignBookingRequest request = new AssignBookingRequest();
        request.setBookingId(booking.getId().toHexString());
        request.setStaffId(staff.getId().toHexString());
        request.setPrice(price);
        return request;
    }
}
