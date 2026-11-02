package com.example.kitechin.service;

import com.example.kitechin.dto.DashboardStats;
import com.example.kitechin.repository.BookingRepository;
import com.example.kitechin.repository.CustomerRepository;
import com.example.kitechin.repository.StaffRepository;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final BookingRepository bookingRepository;
    private final MongoTemplate mongoTemplate;

    public DashboardService(CustomerRepository customerRepository,
                            StaffRepository staffRepository,
                            BookingRepository bookingRepository,
                            MongoTemplate mongoTemplate) {
        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
        this.bookingRepository = bookingRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public DashboardStats getStats() {
        long totalCustomers = customerRepository.count();
        long activeStaff = staffRepository.countByApproved(true);
        long pendingApprovals = staffRepository.countByApproved(false);

        long pendingBookings = 0;
        long assignedBookings = 0;
        long completedBookings = 0;
        long cancelledBookings = 0;
        double revenue = 0;

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("status")
                        .count().as("count")
                        .sum("price").as("priceSum")
        );
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "bookings", Document.class);
        for (Document document : results.getMappedResults()) {
            String status = document.getString("_id");
            Number countValue = document.get("count", Number.class);
            long count = countValue == null ? 0 : countValue.longValue();
            if (status == null) {
                continue;
            }
            switch (status) {
                case "PENDING" -> pendingBookings = count;
                case "ASSIGNED", "CONFIRMED" -> assignedBookings += count;
                case "COMPLETED" -> {
                    completedBookings = count;
                    Number priceSum = document.get("priceSum", Number.class);
                    revenue = priceSum == null ? 0 : priceSum.doubleValue();
                }
                case "CANCELLED" -> cancelledBookings = count;
                default -> {
                }
            }
        }

        return DashboardStats.builder()
                .totalCustomers(totalCustomers)
                .totalStaff(activeStaff + pendingApprovals)
                .activeStaff(activeStaff)
                .pendingApprovals(pendingApprovals)
                .totalBookings(bookingRepository.count())
                .pendingBookings(pendingBookings)
                .assignedBookings(assignedBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .totalRevenue(Math.round(revenue * 100.0) / 100.0)
                .build();
    }
}
