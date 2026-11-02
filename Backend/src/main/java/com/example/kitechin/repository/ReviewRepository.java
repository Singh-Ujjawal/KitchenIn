package com.example.kitechin.repository;

import com.example.kitechin.entity.Review;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, ObjectId> {
    List<Review> findByStaffId(ObjectId staffId);

    boolean existsByBookingId(ObjectId bookingId);

    List<Review> findByBookingIdIn(Collection<ObjectId> bookingIds);
}
