package com.devpulsex.controller;

import com.devpulsex.dto.dashboard.DashboardDto;
import com.devpulsex.dto.dashboard.ProjectMetricsDto;
import com.devpulsex.dto.dashboard.UserMetricsDto;
import com.devpulsex.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/dashboard", "/dashboard"})
@Tag(name = "Dashboard", description = "Project and user metrics and summaries")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/projects")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get project-level metrics")
    public List<ProjectMetricsDto> getProjectMetrics() {
        log.debug("Fetching project metrics");
        return dashboardService.getAllProjectMetrics();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get user-level metrics")
    public List<UserMetricsDto> getUserMetrics() {
        log.debug("Fetching user metrics");
        return dashboardService.getAllUserMetrics();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get dashboard summary")
    public DashboardDto getSummary() {
        log.debug("Fetching dashboard summary");
        return dashboardService.getDashboardSummary();
    }
}
