package com.example.kitechin.repository;

import com.example.kitechin.entity.Booking;
import com.example.kitechin.entity.BookingStatus;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BookingRepository extends MongoRepository<Booking, ObjectId> {
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(ObjectId customerId);

    List<Booking> findByStaffIdOrderByCreatedAtDesc(ObjectId staffId);

    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    boolean existsByStaffId(ObjectId staffId);
}
