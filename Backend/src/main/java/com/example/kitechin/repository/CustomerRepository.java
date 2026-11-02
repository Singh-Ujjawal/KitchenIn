package com.example.kitechin.repository;

import com.example.kitechin.entity.Customer;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CustomerRepository extends MongoRepository<Customer, ObjectId> {
    Optional<Customer> findByEmailIgnoreCase(String email);
}
