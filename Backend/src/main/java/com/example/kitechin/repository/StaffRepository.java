package com.example.kitechin.repository;

import com.example.kitechin.entity.Staff;
import com.example.kitechin.entity.StaffCategory;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends MongoRepository<Staff, ObjectId> {
    Optional<Staff> findByEmailIgnoreCase(String email);

    List<Staff> findByApproved(boolean approved);

    long countByApproved(boolean approved);

    List<Staff> findByCategoryAndApproved(StaffCategory category, boolean approved);

    List<Staff> findByCityIgnoreCaseAndCategoryAndApproved(String city, StaffCategory category, boolean approved);

    List<Staff> findTop6ByApprovedTrueOrderByRatingDesc();
}
