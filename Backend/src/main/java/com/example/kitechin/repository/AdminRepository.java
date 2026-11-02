package com.example.kitechin.repository;

import com.example.kitechin.entity.Admin;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AdminRepository extends MongoRepository<Admin, ObjectId> {
    Optional<Admin> findByEmailIgnoreCase(String email);
}
