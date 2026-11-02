package com.example.kitechin.controller;

import com.example.kitechin.dto.DashboardStats;
import com.example.kitechin.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@Tag(name = "Admin Dashboard APIs")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardStats getStats() {
        return dashboardService.getStats();
    }
}
