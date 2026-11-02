package com.example.kitechin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStats {
    private long totalCustomers;
    private long totalStaff;
    private long activeStaff;
    private long pendingApprovals;
    private long totalBookings;
    private long pendingBookings;
    private long assignedBookings;
    private long completedBookings;
    private long cancelledBookings;
    private Double totalRevenue;
}
