package com.example.kitechin.service;

import com.example.kitechin.common.Ids;
import com.example.kitechin.common.ServiceCities;
import com.example.kitechin.common.TimeSlots;
import com.example.kitechin.dto.AssignBookingRequest;
import com.example.kitechin.dto.BookingRequest;
import com.example.kitechin.dto.BookingResponse;
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
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final ReviewRepository reviewRepository;
    private final MongoTemplate mongoTemplate;

    public BookingService(BookingRepository bookingRepository,
                          CustomerRepository customerRepository,
                          StaffRepository staffRepository,
                          ReviewRepository reviewRepository,
                          MongoTemplate mongoTemplate) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
        this.reviewRepository = reviewRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public BookingResponse createBooking(BookingRequest request, AuthPrincipal principal) {
        if (!TimeSlots.isAllowed(request.getTimeSlot())) {
            throw ApiException.badRequest("Choose a valid time slot");
        }
        ObjectId customerId = Ids.parse(principal.getId(), "customer id");
        if (!customerRepository.existsById(customerId)) {
            throw ApiException.unauthorized("Account not found. Please sign in again.");
        }

        Booking booking = Booking.builder()
                .customerId(customerId)
                .category(request.getCategory())
                .city(ServiceCities.canonical(request.getCity()))
                .bookingDate(request.getBookingDate())
                .timeSlot(request.getTimeSlot())
                .problemDescription(request.getProblemDescription().trim())
                .serviceAddress(request.getServiceAddress().trim())
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        return mapOne(bookingRepository.save(booking));
    }

    public BookingResponse assignStaff(AssignBookingRequest request) {
        requirePositivePrice(request.getPrice());
        ObjectId bookingId = Ids.parse(request.getBookingId(), "booking id");
        ObjectId staffId = Ids.parse(request.getStaffId(), "staff id");

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        requireAssignable(booking);

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> ApiException.notFound("Staff not found"));
        if (!staff.isApproved()) {
            throw ApiException.conflict("Only approved staff can be assigned");
        }
        if (staff.getCategory() != booking.getCategory()) {
            throw ApiException.conflict("Staff category does not match the booking");
        }
        if (!cityMatches(booking, staff)) {
            throw ApiException.conflict("Staff city does not match the booking city");
        }

        Update update = new Update()
                .set("staffId", staff.getId())
                .set("price", request.getPrice())
                .set("status", BookingStatus.ASSIGNED);
        if (booking.getCity() == null || booking.getCity().isBlank()) {
            update.set("city", staff.getCity());
        }

        Booking updated = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(bookingId).and("status").is(BookingStatus.PENDING)),
                update,
                FindAndModifyOptions.options().returnNew(true),
                Booking.class
        );
        if (updated == null) {
            throw ApiException.conflict("This booking was updated by someone else. Refresh and try again.");
        }
        return mapOne(updated);
    }

    public BookingResponse cancelBooking(String bookingId, AuthPrincipal principal) {
        ObjectId id = Ids.parse(bookingId, "booking id");
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));

        boolean admin = "ADMIN".equals(principal.getRole());
        if (!admin && !booking.getCustomerId().equals(Ids.parse(principal.getId(), "customer id"))) {
            throw ApiException.forbidden("You can only cancel your own bookings");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw ApiException.conflict("A completed booking cannot be cancelled");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw ApiException.conflict("This booking is already cancelled");
        }
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.ASSIGNED) {
            throw ApiException.conflict("This booking cannot be cancelled");
        }

        Criteria criteria = Criteria.where("_id").is(id)
                .and("status").in(BookingStatus.PENDING, BookingStatus.ASSIGNED);
        if (!admin) {
            criteria = criteria.and("customerId").is(booking.getCustomerId());
        }
        Booking updated = mongoTemplate.findAndModify(
                Query.query(criteria),
                new Update().set("status", BookingStatus.CANCELLED),
                FindAndModifyOptions.options().returnNew(true),
                Booking.class
        );
        if (updated == null) {
            throw ApiException.conflict("This booking was updated by someone else. Refresh and try again.");
        }
        return mapOne(updated);
    }

    public BookingResponse completeBooking(String bookingId, AuthPrincipal principal) {
        ObjectId id = Ids.parse(bookingId, "booking id");
        ObjectId staffId = Ids.parse(principal.getId(), "staff id");
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw ApiException.conflict("This booking is already completed");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw ApiException.conflict("A cancelled booking cannot be completed");
        }
        if (booking.getStatus() != BookingStatus.ASSIGNED) {
            throw ApiException.conflict("Only an assigned booking can be completed");
        }
        if (booking.getStaffId() == null || !booking.getStaffId().equals(staffId)) {
            throw ApiException.forbidden("You can only complete bookings assigned to you");
        }

        Booking updated = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(id)
                        .and("status").is(BookingStatus.ASSIGNED)
                        .and("staffId").is(staffId)),
                new Update().set("status", BookingStatus.COMPLETED),
                FindAndModifyOptions.options().returnNew(true),
                Booking.class
        );
        if (updated == null) {
            throw ApiException.conflict("This booking was updated by someone else. Refresh and try again.");
        }
        return mapOne(updated);
    }

    public List<BookingResponse> getCustomerBookings(String customerId, AuthPrincipal principal) {
        ObjectId id = Ids.parse(customerId, "customer id");
        if ("CUSTOMER".equals(principal.getRole()) && !id.equals(Ids.parse(principal.getId(), "customer id"))) {
            throw ApiException.forbidden("You can only view your own bookings");
        }
        if (!customerRepository.existsById(id)) {
            throw ApiException.notFound("Customer not found");
        }
        return mapAll(bookingRepository.findByCustomerIdOrderByCreatedAtDesc(id));
    }

    public List<BookingResponse> getStaffBookings(String staffId, AuthPrincipal principal) {
        ObjectId id = Ids.parse(staffId, "staff id");
        if ("WORKER".equals(principal.getRole()) && !id.equals(Ids.parse(principal.getId(), "staff id"))) {
            throw ApiException.forbidden("You can only view your own jobs");
        }
        if (!staffRepository.existsById(id)) {
            throw ApiException.notFound("Staff not found");
        }
        return mapAll(bookingRepository.findByStaffIdOrderByCreatedAtDesc(id));
    }

    public List<BookingResponse> getPendingBookings() {
        return mapAll(bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.PENDING));
    }

    private void requireAssignable(Booking booking) {
        if (booking.getStatus() == BookingStatus.ASSIGNED) {
            throw ApiException.conflict("This booking is already assigned");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw ApiException.conflict("A completed booking cannot be assigned");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw ApiException.conflict("A cancelled booking cannot be assigned");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw ApiException.conflict("Only pending bookings can be assigned");
        }
    }

    private void requirePositivePrice(Double price) {
        if (price == null || !Double.isFinite(price) || price <= 0 || price > 1_000_000) {
            throw ApiException.badRequest("Price must be greater than 0 and at most 1000000");
        }
    }

    private boolean cityMatches(Booking booking, Staff staff) {
        if (staff.getCity() == null || staff.getCity().isBlank()) {
            return false;
        }
        if (booking.getCity() != null && !booking.getCity().isBlank()) {
            return booking.getCity().equalsIgnoreCase(staff.getCity());
        }
        String address = booking.getServiceAddress() == null ? "" : booking.getServiceAddress().toLowerCase(Locale.ROOT);
        return address.contains(staff.getCity().toLowerCase(Locale.ROOT));
    }

    private BookingResponse mapOne(Booking booking) {
        return mapAll(List.of(booking)).get(0);
    }

    private List<BookingResponse> mapAll(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return List.of();
        }
        Set<ObjectId> customerIds = new HashSet<>();
        Set<ObjectId> staffIds = new HashSet<>();
        Set<ObjectId> bookingIds = new HashSet<>();
        for (Booking booking : bookings) {
            if (booking.getCustomerId() != null) {
                customerIds.add(booking.getCustomerId());
            }
            if (booking.getStaffId() != null) {
                staffIds.add(booking.getStaffId());
            }
            if (booking.getId() != null) {
                bookingIds.add(booking.getId());
            }
        }

        Map<ObjectId, Customer> customers = new HashMap<>();
        if (!customerIds.isEmpty()) {
            customerRepository.findAllById(customerIds).forEach(customer -> customers.put(customer.getId(), customer));
        }
        Map<ObjectId, Staff> staffMembers = new HashMap<>();
        if (!staffIds.isEmpty()) {
            staffRepository.findAllById(staffIds).forEach(staff -> staffMembers.put(staff.getId(), staff));
        }
        Set<ObjectId> reviewed = new HashSet<>();
        if (!bookingIds.isEmpty()) {
            for (Review review : reviewRepository.findByBookingIdIn(bookingIds)) {
                reviewed.add(review.getBookingId());
            }
        }

        return bookings.stream()
                .map(booking -> toResponse(booking, customers, staffMembers, reviewed))
                .toList();
    }

    private BookingResponse toResponse(Booking booking,
                                       Map<ObjectId, Customer> customers,
                                       Map<ObjectId, Staff> staffMembers,
                                       Set<ObjectId> reviewed) {
        Customer customer = customers.get(booking.getCustomerId());
        Staff staff = booking.getStaffId() == null ? null : staffMembers.get(booking.getStaffId());
        return BookingResponse.builder()
                .id(booking.getId() == null ? null : booking.getId().toHexString())
                .customerId(booking.getCustomerId() == null ? null : booking.getCustomerId().toHexString())
                .customerName(customer == null ? "Customer" : customer.getName())
                .customerPhone(customer == null ? null : customer.getPhone())
                .staffId(booking.getStaffId() == null ? null : booking.getStaffId().toHexString())
                .staffName(staff == null ? null : staff.getName())
                .staffPhone(staff == null ? null : staff.getPhone())
                .staffRating(staff == null ? null : staff.getRating())
                .staffReviewCount(staff == null ? null : staff.getReviewCount())
                .staffProfilePicture(staff == null ? null : staff.getProfilePicture())
                .category(booking.getCategory())
                .city(booking.getCity())
                .bookingDate(booking.getBookingDate())
                .timeSlot(booking.getTimeSlot())
                .problemDescription(booking.getProblemDescription())
                .serviceAddress(booking.getServiceAddress())
                .price(booking.getPrice())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .reviewed(booking.getId() != null && reviewed.contains(booking.getId()))
                .build();
    }
}
